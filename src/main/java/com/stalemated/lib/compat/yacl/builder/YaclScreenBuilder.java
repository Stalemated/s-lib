package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.internal.YaclBindingSynchronizer;
import com.stalemated.lib.compat.yacl.internal.YaclContext;
import com.stalemated.lib.compat.yacl.registry.OptionAvailabilityRule;
import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.compat.yacl.registry.YaclNamingStrategy;
import com.stalemated.lib.config.ConfigManager;
import com.stalemated.lib.config.Subscription;
import com.stalemated.lib.config.SyncedConfigManager;
import com.stalemated.lib.config.internal.ConfigSession;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.utils.OptionUtils;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Builds a YACL config screen for one or more {@link ConfigManager}s. Client only.
 * <p>
 * Each manager gets a draft: edits stay there until the player saves, then each changed config
 * is saved once. If a save fails, its edits stay pending. The tabs are built in
 * {@link #buildLib()} and {@link #generateScreen()}, so
 * {@link #namingStrategy(YaclNamingStrategy)}, {@link #controllerRegistry(YaclControllerRegistry)}
 * and {@link #availabilityRule(OptionAvailabilityRule)} apply to every tab whatever the order of
 * the calls.
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

    /**
     * Creates a new screen builder.
     *
     * @param parent the screen to return to when closing, or {@code null}
     * @param title the screen title displayed by YACL
     * @param translationPrefix prefix of every generated translation key, usually the mod id
     * @return a new {@link YaclScreenBuilder}
     * @throws NullPointerException if {@code title} or {@code translationPrefix} is {@code null}
     */
    public static YaclScreenBuilder create(Screen parent, Component title, String translationPrefix) {
        return new YaclScreenBuilder(parent, title, translationPrefix);
    }

    /**
     * Sets the naming strategy that makes the translation keys of the screen. The default is
     * {@link YaclNamingStrategy#DEFAULT}.
     *
     * @param namingStrategy the strategy to use
     * @return this builder
     * @throws NullPointerException if {@code namingStrategy} is {@code null}
     */
    public YaclScreenBuilder namingStrategy(YaclNamingStrategy namingStrategy) {
        this.namingStrategy = Objects.requireNonNull(namingStrategy, "namingStrategy cannot be null");
        return this;
    }

    /**
     * Sets the registry that picks the controller of each option type, for example one with your
     * own types registered. By default each builder gets its own
     * {@link YaclControllerRegistry#createDefault()}. A registry can be shared between screens.
     *
     * @param controllerRegistry the registry to use
     * @return this builder
     * @throws NullPointerException if {@code controllerRegistry} is {@code null}
     */
    public YaclScreenBuilder controllerRegistry(YaclControllerRegistry controllerRegistry) {
        this.controllerRegistry = Objects.requireNonNull(controllerRegistry, "controllerRegistry cannot be null");
        return this;
    }

    /**
     * Sets the rule that locks options and gives the reason shown under their description. The
     * default is {@link OptionAvailabilityRule#DEFAULT}; to keep its permission lock, pass
     * {@code OptionAvailabilityRule.DEFAULT.and(yourRule)}.
     * <p>
     * When the screen rebuilds after a change made elsewhere, it drops the pending edits of the
     * options this rule locks.
     *
     * @param availabilityRule the rule to use
     * @return this builder
     * @throws NullPointerException if {@code availabilityRule} is {@code null}
     */
    public YaclScreenBuilder availabilityRule(OptionAvailabilityRule availabilityRule) {
        this.availabilityRule = Objects.requireNonNull(availabilityRule, "availabilityRule cannot be null");
        return this;
    }

    /**
     * Binds a preview object to the draft of {@code manager}. Every pending change on the screen
     * updates {@code previewInstance} right away and calls {@code onPreviewUpdate}, without
     * touching the draft. When the screen rebuilds, the preview gets the draft values again. A
     * second call for the same manager replaces the preview.
     *
     * @param manager the manager whose options drive the preview, the same one passed to {@code category}
     * @param previewInstance object that receives every pending edit; {@code null} removes the preview
     * @param onPreviewUpdate called after each update, for example {@code TooltipEntry::invalidateCaches}, or {@code null}
     * @param <T> the config model type
     * @return this builder
     * @throws NullPointerException if {@code manager} is {@code null}
     * @throws IllegalStateException if {@code manager} is not registered yet
     */
    public <T> YaclScreenBuilder livePreview(
            ConfigManager<T> manager,
            T previewInstance,
            Consumer<T> onPreviewUpdate
    ) {
        sessionOf(manager).bindPreview(previewInstance, onPreviewUpdate);
        return this;
    }

    /**
     * Adds a tab for {@code manager}. Its name comes from
     * {@link YaclNamingStrategy#categoryName(String, String)}, {@code <prefix>.category.<tabKey>}
     * by default, and its tooltip from {@code <prefix>.category.<tabKey>.desc}. Tabs of the same
     * manager share one draft.
     * <p>
     * {@code contents} fills the tab every time the screen is built or rebuilt (save, undo,
     * adding or removing entries, server changes), so it must be safe to run more than once. Its
     * errors, and a {@code null} {@code tabKey} or {@code contents}, show up when the screen is
     * built.
     *
     * @param manager the config to edit
     * @param tabKey key of the tab
     * @param contents fills the tab, for example {@code YaclCategoryBuilder::autoGenerate}
     * @param <T> the config model type
     * @return this builder
     * @throws NullPointerException if {@code manager} is {@code null}
     * @throws IllegalStateException if {@code manager} is not registered yet
     */
    public <T> YaclScreenBuilder category(ConfigManager<T> manager, String tabKey, Consumer<YaclCategoryBuilder<T>> contents) {
        ConfigSession<T> session = sessionOf(manager);
        categories.add(synchronizers -> buildCategory(session, synchronizers.computeIfAbsent(session, YaclBindingSynchronizer::new), tabKey, contents));
        return this;
    }

    /**
     * Adds a YACL category made by {@code customCategory}. The supplier runs every time the
     * screen is built or rebuilt, so it must create a new category on each call. Its options are
     * saved by their own YACL bindings, not by S-Lib.
     *
     * @param customCategory makes the category
     * @return this builder
     * @throws NullPointerException if {@code customCategory} is {@code null}
     */
    public YaclScreenBuilder category(Supplier<ConfigCategory> customCategory) {
        Objects.requireNonNull(customCategory, "customCategory cannot be null");
        categories.add(synchronizers -> customCategory.get());
        return this;
    }

    record ScreenBuild(YetAnotherConfigLib lib, Collection<YaclBindingSynchronizer> synchronizers, Map<SyncedConfigManager<?>, Boolean> overridePermissions) {}

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

    /**
     * Builds the tabs and returns the YACL object, without a screen. It is mostly for tests, like
     * {@code SLibTesting}. A screen made with {@code lib.generateScreen(parent)} doesn't rebuild
     * itself, and its Add Entry and Remove Entry buttons throw an {@code IllegalStateException}.
     * <p>
     * Errors from the tab contents, like an unknown option key, are thrown here.
     *
     * @return the YACL object; saving it saves the drafts
     * @throws IllegalStateException if two options of the same config get the same name
     */
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
        return new ScreenBuild(lib, List.copyOf(synchronizers.values()), overridePermissions());
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
        sessions.values().forEach(session -> {
            session.catchUpWithManager();
            session.discardChanges(option -> availabilityRule.unavailableReason(option, session.getManager()).isPresent());
        });
    }

    Subscription onViewChange(Runnable listener) {
        List<Subscription> subscriptions = syncedManagers().map(manager -> manager.onViewChange(listener)).toList();
        return () -> subscriptions.forEach(Subscription::unsubscribe);
    }

    Subscription onConfigChange(Runnable listener) {
        List<Subscription> subscriptions = sessions.keySet().stream().map(manager -> manager.onChange(config -> listener.run())).toList();
        return () -> subscriptions.forEach(Subscription::unsubscribe);
    }

    boolean overridePermissionsChangedSince(ScreenBuild build) {
        return !build.overridePermissions().equals(overridePermissions());
    }

    private Map<SyncedConfigManager<?>, Boolean> overridePermissions() {
        return syncedManagers().collect(Collectors.toMap(Function.identity(), SyncedConfigManager::canChangeOverrides));
    }

    private Stream<SyncedConfigManager<?>> syncedManagers() {
        return sessions.keySet().stream()
                .filter(SyncedConfigManager.class::isInstance)
                .map(manager -> (SyncedConfigManager<?>) manager);
    }

    static void requestDefaultsOfAvailableOptions(YetAnotherConfigLib lib) {
        OptionUtils.getFlatOptions(lib).filter(Option::available).forEach(Option::requestSetDefault);
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

    /**
     * Builds the S-Lib config screen. It rebuilds itself after save, undo, adding or removing
     * entries, and when the server or another player changes the config, keeping the open tab,
     * the scroll and the open groups.
     * <p>
     * Errors from the tab contents, like an unknown option key, are thrown here.
     *
     * @return the new screen
     * @throws IllegalStateException if two options of the same config get the same name
     */
    public Screen generateScreen() {
        return new YaclConfigScreen(this, build(), parent, YaclScreenView.FIRST_TAB);
    }
}
