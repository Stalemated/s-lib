package com.stalemated.lib.mixin.internal.client.yacl;

import com.stalemated.lib.mixin.RequiresMod;
import dev.isxander.yacl3.gui.OptionListWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@RequiresMod("yet_another_config_lib_v3")
@Mixin(ContainerObjectSelectionList.Entry.class)
public abstract class OptionEntryFocusMixin {

    @Shadow
    private GuiEventListener focused;

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "setFocused(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At("HEAD"), cancellable = true)
    private void s_lib$keepFocusedChild(GuiEventListener child, CallbackInfo ci) {
        if (child != null && child == focused && (Object) this instanceof OptionListWidget.Entry) {
            if (!child.isFocused()) {
                child.setFocused(true);
            }
            ci.cancel();
        }
    }
}
