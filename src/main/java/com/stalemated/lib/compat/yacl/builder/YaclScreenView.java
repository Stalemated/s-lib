package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.mixin.internal.client.yacl.CategoryTabAccessor;
//? if yacl: <3.8.0 {
import com.stalemated.lib.mixin.internal.client.yacl.ElementListWidgetExtInvoker;
//?}
import com.stalemated.lib.mixin.internal.client.yacl.GroupSeparatorEntryAccessor;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.gui.OptionListWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.components.tabs.Tab;
//? if yacl: <3.8.0 {
import net.minecraft.client.gui.navigation.ScreenRectangle;
//?}

import java.util.List;

record YaclScreenView(int tab, double scroll, List<GroupExpansion> groupsByTab) {
    static final YaclScreenView FIRST_TAB = new YaclScreenView(0, 0, List.of());

    static YaclScreenView of(YACLScreen screen) {
        List<Tab> tabs = screen.tabNavigationBar.getTabs();
        Tab current = screen.tabManager.getCurrentTab();
        int tab = Math.max(tabs.indexOf(current), 0);
        double scroll = current instanceof YACLScreen.CategoryTab categoryTab ? scrollOf(optionListOf(categoryTab)) : 0;
        List<GroupExpansion> groupsByTab = tabs.stream().map(YaclScreenView::expansionOf).toList();
        return new YaclScreenView(tab, scroll, groupsByTab);
    }

    void restore(YACLScreen screen) {
        List<Tab> tabs = screen.tabNavigationBar.getTabs();
        for (int index = 0; index < Math.min(groupsByTab.size(), tabs.size()); index++) {
            if (tabs.get(index) instanceof YACLScreen.CategoryTab categoryTab) {
                restoreGroups(optionListOf(categoryTab), groupsByTab.get(index));
            }
        }
        if (tab < tabs.size() && tabs.get(tab) instanceof YACLScreen.CategoryTab categoryTab) {
            restoreScroll(screen, optionListOf(categoryTab));
        }
    }

    private void restoreScroll(YACLScreen screen, OptionListWidget list) {
        //? if yacl: <3.8.0 {
        list.updateDimensions(new ScreenRectangle(screen.tabArea.position(), screen.tabArea.width() / 3 * 2, screen.tabArea.height()));
        list.setScrollAmount(scroll);
        ((ElementListWidgetExtInvoker) list).s_lib$resetSmoothScrolling();
        //?} else {
        /*list.setScrollAmount(scroll);
        *///?}
    }

    private static GroupExpansion expansionOf(Tab tab) {
        if (!(tab instanceof YACLScreen.CategoryTab categoryTab)) {
            return GroupExpansion.NONE;
        }
        List<OptionListWidget.GroupSeparatorEntry> separators = separatorsOf(optionListOf(categoryTab));
        return GroupExpansion.of(namesOf(separators), separators.stream().map(YaclScreenView::isExpandedByUser).toList());
    }

    private static boolean isExpandedByUser(OptionListWidget.GroupSeparatorEntry separator) {
        if (isUnavailableList(separator)) {
            return !((GroupSeparatorEntryAccessor) separator).s_lib$group().collapsed();
        }
        return separator.isExpanded();
    }

    private static boolean isUnavailableList(OptionListWidget.GroupSeparatorEntry separator) {
        return ((GroupSeparatorEntryAccessor) separator).s_lib$group() instanceof ListOption<?> list && !list.available();
    }

    private static void restoreGroups(OptionListWidget list, GroupExpansion saved) {
        for (int index = 0; index < separatorsOf(list).size(); index++) {
            List<OptionListWidget.GroupSeparatorEntry> separators = separatorsOf(list);
            OptionListWidget.GroupSeparatorEntry separator = separators.get(index);
            if (isUnavailableList(separator)) continue;
            saved.expandedAt(namesOf(separators), index).ifPresent(separator::setExpanded);
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
