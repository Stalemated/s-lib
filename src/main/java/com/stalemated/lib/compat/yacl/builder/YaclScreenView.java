package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.mixin.internal.client.yacl.CategoryTabAccessor;
import com.stalemated.lib.mixin.internal.client.yacl.GroupSeparatorEntryAccessor;
import dev.isxander.yacl3.gui.OptionListWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.components.tabs.Tab;

import java.util.List;

record YaclScreenView(int tab, double scroll, List<GroupExpansion> groupsByTab) {
    static final YaclScreenView FIRST_TAB = new YaclScreenView(0, 0, List.of());

    static YaclScreenView of(List<Tab> tabs, Tab current) {
        int tab = Math.max(tabs.indexOf(current), 0);
        double scroll = current instanceof YACLScreen.CategoryTab categoryTab ? scrollOf(optionListOf(categoryTab)) : 0;
        List<GroupExpansion> groupsByTab = tabs.stream().map(YaclScreenView::expansionOf).toList();
        return new YaclScreenView(tab, scroll, groupsByTab);
    }

    void restore(List<Tab> tabs) {
        for (int index = 0; index < Math.min(groupsByTab.size(), tabs.size()); index++) {
            if (tabs.get(index) instanceof YACLScreen.CategoryTab categoryTab) {
                restoreGroups(optionListOf(categoryTab), groupsByTab.get(index));
            }
        }
        if (tab < tabs.size() && tabs.get(tab) instanceof YACLScreen.CategoryTab categoryTab) {
            optionListOf(categoryTab).setScrollAmount(scroll);
        }
    }

    private static GroupExpansion expansionOf(Tab tab) {
        if (!(tab instanceof YACLScreen.CategoryTab categoryTab)) {
            return GroupExpansion.NONE;
        }
        List<OptionListWidget.GroupSeparatorEntry> separators = separatorsOf(optionListOf(categoryTab));
        return GroupExpansion.of(namesOf(separators), separators.stream().map(OptionListWidget.GroupSeparatorEntry::isExpanded).toList());
    }

    private static void restoreGroups(OptionListWidget list, GroupExpansion saved) {
        for (int index = 0; index < separatorsOf(list).size(); index++) {
            List<OptionListWidget.GroupSeparatorEntry> separators = separatorsOf(list);
            saved.expandedAt(namesOf(separators), index).ifPresent(separators.get(index)::setExpanded);
        }
    }

    private static List<OptionListWidget.GroupSeparatorEntry> separatorsOf(OptionListWidget list) {
        return list.children().stream()
                .filter(OptionListWidget.GroupSeparatorEntry.class::isInstance)
                .map(OptionListWidget.GroupSeparatorEntry.class::cast)
                .toList();
    }

    private static List<String> namesOf(List<OptionListWidget.GroupSeparatorEntry> separators) {
        return separators.stream()
                .map(separator -> ((GroupSeparatorEntryAccessor) separator).s_lib$group().name().getString())
                .toList();
    }

    private static OptionListWidget optionListOf(YACLScreen.CategoryTab tab) {
        //? if yacl: <3.8.0 {
        return ((CategoryTabAccessor) tab).s_lib$optionList().getList();
        //?} else {
        /*return ((CategoryTabAccessor) tab).s_lib$optionList().getType();
        *///?}
    }

    private static double scrollOf(OptionListWidget list) {
        //? if <1.21.4 {
        return list.getScrollAmount();
        //?} else {
        /*return list.scrollAmount();
        *///?}
    }
}
