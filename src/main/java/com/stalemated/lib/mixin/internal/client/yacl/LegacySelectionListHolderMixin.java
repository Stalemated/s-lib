package com.stalemated.lib.mixin.internal.client.yacl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.PrintStream;

@Pseudo
@Mixin(targets = "dev.isxander.yacl3.gui.LegacySelectionList$Holder")
public abstract class LegacySelectionListHolderMixin {

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ljava/io/PrintStream;println(Ljava/lang/String;)V"), require = 0)
    private void s_lib$dropScrollDebugLine(PrintStream out, String line) {
    }
}
