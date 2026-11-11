package com.stalemated.lib.mixin.internal.client.yacl;

import com.stalemated.lib.mixin.RequiresMod;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.gui.OptionListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@RequiresMod("yet_another_config_lib_v3")
@Mixin(OptionListWidget.GroupSeparatorEntry.class)
public interface GroupSeparatorEntryAccessor {

    @Accessor("group")
    OptionGroup s_lib$group();
}
