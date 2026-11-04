package com.stalemated.lib.mixin;

import com.stalemated.lib.helper.PlatformHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ConditionalMixinPlugin implements IMixinConfigPlugin {

    private static final String REQUIRES_MOD = Type.getDescriptor(RequiresMod.class);

    private final Predicate<String> isModPresent;
    private final Function<String, ClassNode> mixinClasses;

    public ConditionalMixinPlugin() {
        this(PlatformHelper::isModLoadedAtLaunch, ConditionalMixinPlugin::readMixin);
    }

    ConditionalMixinPlugin(Predicate<String> isModPresent, Function<String, ClassNode> mixinClasses) {
        this.isModPresent = Objects.requireNonNull(isModPresent, "isModPresent cannot be null");
        this.mixinClasses = Objects.requireNonNull(mixinClasses, "mixinClasses cannot be null");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return requiredMods(mixinClasses.apply(mixinClassName)).stream().allMatch(isModPresent);
    }

    private static List<String> requiredMods(ClassNode mixin) {
        if (mixin.invisibleAnnotations == null) return List.of();
        return mixin.invisibleAnnotations.stream()
                .filter(annotation -> REQUIRES_MOD.equals(annotation.desc))
                .findFirst()
                .map(ConditionalMixinPlugin::modIds)
                .orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    private static List<String> modIds(AnnotationNode requiresMod) {
        return (List<String>) requiresMod.values.get(1);
    }

    private static ClassNode readMixin(String mixinClassName) {
        try {
            return MixinService.getService().getBytecodeProvider().getClassNode(mixinClassName, false);
        } catch (ClassNotFoundException | IOException e) {
            throw new IllegalStateException("Cannot read mixin " + mixinClassName + " to check @RequiresMod", e);
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
