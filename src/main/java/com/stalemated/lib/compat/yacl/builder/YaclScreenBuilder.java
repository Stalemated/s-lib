package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.factory.YaclContext;
import com.stalemated.lib.compat.yacl.registry.OptionAvailabilityRule;
import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.compat.yacl.registry.YaclNamingStrategy;
import com.stalemated.lib.compat.yacl.util.YaclBindingSynchronizer;
import com.stalemated.lib.config.LocalConfigManager;
import com.stalemated.lib.config.session.ConfigSession;
import com.stalemated.lib.config.session.ConfigSessionCoordinator;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Top-level fluent builder for creating YACL configuration screens backed by atomic session buffers.
 * Coordinates multi-manager sessions and orchestrates category composition.
 */
@SuppressWarnings("UnusedReturnValue")
public class YaclScreenBuilder {
    private final Screen parent;
    private final Component title;
    private final ConfigSessionCoordinator coordinator = new ConfigSessionCoordinator();
    private final Map<ConfigSession<?>, YaclBindingSynchronizer> synchronizers = new HashMap<>();
    private final List<ConfigCategory> categories = new ArrayList<>();
    private final String translationPrefix;
    private YaclNamingStrategy namingStrategy = YaclNamingStrategy.DEFAULT;
    private YaclControllerRegistry controllerRegistry = YaclControllerRegistry.createDefault();
    private OptionAvailabilityRule availabilityRule = OptionAvailabilityRule.DEFAULT;

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
            LocalConfigManager<T> manager,
            T previewInstance,
            Consumer<T> onPreviewUpdate
    ) {
        ConfigSession<T> session = coordinator.getOrCreate(manager);
        session.bindPreview(previewInstance, onPreviewUpdate);
        return this;
    }

    private <T> YaclCategoryBuilder<T> createCategoryBuilder(LocalConfigManager<T> manager) {
        ConfigSession<T> session = coordinator.getOrCreate(manager);
        YaclBindingSynchronizer synchronizer = synchronizers.computeIfAbsent(session, YaclBindingSynchronizer::new);
        return new YaclCategoryBuilder<>(new YaclContext<>(session, synchronizer, translationPrefix, namingStrategy, controllerRegistry, availabilityRule));
    }

    public <T> YaclScreenBuilder category(LocalConfigManager<T> manager, String tabKey, Consumer<YaclCategoryBuilder<T>> contents) {
        YaclCategoryBuilder<T> builder = createCategoryBuilder(manager)
                .name(namingStrategy.resolveCategoryName(translationPrefix, tabKey))
                .description(namingStrategy.resolveCategoryDescription(translationPrefix, tabKey));
        contents.accept(builder);
        categories.add(builder.build());
        return this;
    }

    public YaclScreenBuilder category(ConfigCategory customCategory) {
        this.categories.add(customCategory);
        return this;
    }

    public ConfigSessionCoordinator getCoordinator() {
        return coordinator;
    }

    public YetAnotherConfigLib buildLib() {
        return YetAnotherConfigLib.createBuilder()
                .title(title)
                .categories(categories)
                .save(coordinator::commitAll)
                .build();
    }

    public Screen generateScreen() {
        return buildLib().generateScreen(parent);
    }
}
