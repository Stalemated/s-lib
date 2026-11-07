package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.internal.YaclBindingSynchronizer;
import com.stalemated.lib.compat.yacl.internal.YaclContext;
import com.stalemated.lib.compat.yacl.registry.OptionAvailabilityRule;
import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.compat.yacl.registry.YaclNamingStrategy;
import com.stalemated.lib.config.ConfigManager;
import com.stalemated.lib.config.internal.ConfigSession;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Top-level fluent builder for creating YACL configuration screens backed by atomic session buffers.
 * Keeps one session per config manager and commits all of them when the screen saves.
 * Categories are built in {@link #buildLib()}, so the naming strategy, controller registry, and availability rule
 * apply to every category whatever the order of the calls.
 */
public class YaclScreenBuilder {
    private final Screen parent;
    private final Component title;
    private final String translationPrefix;
    private final Map<ConfigManager<?>, ConfigSession<?>> sessions = new LinkedHashMap<>();
    private final List<PendingCategory> categories = new ArrayList<>();
    private YaclNamingStrategy namingStrategy = YaclNamingStrategy.DEFAULT;
    private YaclControllerRegistry controllerRegistry = YaclControllerRegistry.createDefault();
    private OptionAvailabilityRule availabilityRule = OptionAvailabilityRule.DEFAULT;

    @FunctionalInterface
    private interface PendingCategory {
        ConfigCategory build(Map<ConfigSession<?>, YaclBindingSynchronizer> synchronizers);
    }

    private YaclScreenBuilder(Screen parent, Component title, String translationPrefix) {
        this.parent = parent;
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.translationPrefix = Objects.requireNonNull(translationPrefix, "translationPrefix cannot be null");
    }

    public static YaclScreenBuilder create(Screen parent, Component title, String translationPrefix) {
        return new YaclScreenBuilder(parent, title, translationPrefix);
    }

    public YaclScreenBuilder namingStrategy(YaclNamingStrategy namingStrategy) {
        this.namingStrategy = Objects.requireNonNull(namingStrategy, "namingStrategy cannot be null");
        return this;
    }

    public YaclScreenBuilder controllerRegistry(YaclControllerRegistry controllerRegistry) {
        this.controllerRegistry = Objects.requireNonNull(controllerRegistry, "controllerRegistry cannot be null");
        return this;
    }

    public YaclScreenBuilder availabilityRule(OptionAvailabilityRule availabilityRule) {
        this.availabilityRule = Objects.requireNonNull(availabilityRule, "availabilityRule cannot be null");
        return this;
    }

    /**
     * Binds a live preview instance to the session for {@code manager} so any pending option change
     * in the YACL screen immediately updates {@code previewInstance} and invokes {@code onPreviewUpdate}
     * without mutating the uncommitted session draft.
     *
     * @param manager         The config manager whose session should drive the preview.
     * @param previewInstance The live preview instance to mutate on pending UI changes.
     * @param onPreviewUpdate Optional callback invoked after each preview update (e.g., {@code TooltipEntry::invalidateCaches}).
     * @param <T>             The config model type.
     * @return This screen builder.
     */
    public <T> YaclScreenBuilder livePreview(
            ConfigManager<T> manager,
            T previewInstance,
            Consumer<T> onPreviewUpdate
    ) {
        sessionOf(manager).bindPreview(previewInstance, onPreviewUpdate);
        return this;
    }

    public <T> YaclScreenBuilder category(ConfigManager<T> manager, String tabKey, Consumer<YaclCategoryBuilder<T>> contents) {
        ConfigSession<T> session = sessionOf(manager);
        categories.add(synchronizers -> buildCategory(session, synchronizers.computeIfAbsent(session, YaclBindingSynchronizer::new), tabKey, contents));
        return this;
    }

    public YaclScreenBuilder category(ConfigCategory customCategory) {
        categories.add(synchronizers -> customCategory);
        return this;
    }

    record ScreenBuild(YetAnotherConfigLib lib, Collection<YaclBindingSynchronizer> synchronizers) {}

    @SuppressWarnings("unchecked")
    <T> ConfigSession<T> sessionOf(ConfigManager<T> manager) {
        Objects.requireNonNull(manager, "manager cannot be null");
        return (ConfigSession<T>) sessions.computeIfAbsent(manager, ConfigSession::new);
    }

    private <T> ConfigCategory buildCategory(
            ConfigSession<T> session,
            YaclBindingSynchronizer synchronizer,
            String tabKey,
            Consumer<YaclCategoryBuilder<T>> contents
    ) {
        YaclContext<T> context = new YaclContext<>(session, synchronizer, translationPrefix, namingStrategy, controllerRegistry, availabilityRule);
        YaclCategoryBuilder<T> builder = new YaclCategoryBuilder<>(context)
                .name(namingStrategy.categoryName(translationPrefix, tabKey))
                .description(namingStrategy.categoryDescription(translationPrefix, tabKey));
        contents.accept(builder);
        return builder.build();
    }

    public YetAnotherConfigLib buildLib() {
        return build().lib();
    }

    ScreenBuild build() {
        controllerRegistry.clearItemTargets();
        Map<ConfigSession<?>, YaclBindingSynchronizer> synchronizers = new HashMap<>();
        List<ConfigCategory> builtCategories = new ArrayList<>(categories.size());
        for (PendingCategory category : categories) {
            builtCategories.add(category.build(synchronizers));
        }
        YetAnotherConfigLib lib = YetAnotherConfigLib.createBuilder()
                .title(title)
                .categories(builtCategories)
                .save(this::commitSessions)
                .build();
        return new ScreenBuild(lib, List.copyOf(synchronizers.values()));
    }

    ScreenBuild rebuild(ScreenBuild current, Runnable change) {
        current.synchronizers().forEach(YaclBindingSynchronizer::applyPendingValues);
        change.run();
        sessions.values().forEach(ConfigSession::syncPreviewWithDraft);
        return build();
    }

    boolean sessionsHaveChanges() {
        return sessions.values().stream().anyMatch(ConfigSession::hasChanges);
    }

    boolean sessionsBehindManagers() {
        return sessions.values().stream().anyMatch(ConfigSession::isBehindManager);
    }

    void catchUpSessions() {
        sessions.values().forEach(ConfigSession::catchUpWithManager);
    }

    void discardSessionChanges() {
        sessions.values().forEach(ConfigSession::discardChanges);
    }

    private void commitSessions() {
        for (ConfigSession<?> session : sessions.values()) {
            try {
                session.commit();
            } catch (UncheckedIOException e) {
                LOGGER.error("Kept the screen changes of '{}' pending because they could not be saved: {}", session.getManager().getName(), e.getMessage());
            }
        }
    }

    public Screen generateScreen() {
        return new YaclConfigScreen(this, build(), parent, 0);
    }
}
