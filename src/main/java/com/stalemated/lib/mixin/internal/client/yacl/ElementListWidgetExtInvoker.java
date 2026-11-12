package com.stalemated.lib.mixin.internal.client.yacl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "dev.isxander.yacl3.gui.ElementListWidgetExt")
public interface ElementListWidgetExtInvoker {

    @Invoker("resetSmoothScrolling")
    void s_lib$resetSmoothScrolling();
}
