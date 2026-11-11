package com.stalemated.lib.mixin.internal.client.yacl;

import com.stalemated.lib.mixin.RequiresMod;
import dev.isxander.yacl3.gui.OptionListWidget;
import dev.isxander.yacl3.gui.YACLScreen;
//? if yacl: <3.8.0 {
import dev.isxander.yacl3.gui.tab.ListHolderWidget;
//?} else {
/*import dev.isxander.yacl3.gui.WidgetAndType;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@RequiresMod("yet_another_config_lib_v3")
@Mixin(YACLScreen.CategoryTab.class)
public interface CategoryTabAccessor {

    //? if yacl: <3.8.0 {
    @Accessor("optionList")
    ListHolderWidget<OptionListWidget> s_lib$optionList();
    //?} else {
    /*@Accessor("optionList")
    WidgetAndType<OptionListWidget> s_lib$optionList();
    *///?}
}
