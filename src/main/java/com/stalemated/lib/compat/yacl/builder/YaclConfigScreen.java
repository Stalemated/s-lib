package com.stalemated.lib.compat.yacl.builder;

import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.screens.Screen;

final class YaclConfigScreen extends YACLScreen {
    private final YaclScreenBuilder owner;
    private final YaclScreenBuilder.ScreenBuild build;
    private final Screen parent;
    private int openingTab;

    YaclConfigScreen(YaclScreenBuilder owner, YaclScreenBuilder.ScreenBuild build, Screen parent, int openingTab) {
        super(build.lib(), parent);
        this.owner = owner;
        this.build = build;
        this.parent = parent;
        this.openingTab = openingTab;
    }

    static YaclConfigScreen from(YACLScreen screen) {
        if (screen instanceof YaclConfigScreen configScreen) {
            return configScreen;
        }
        throw new IllegalStateException("Remove Entry needs the screen from YaclScreenBuilder.generateScreen(), got " + screen.getClass().getName());
    }

    @Override
    protected void init() {
        super.init();
        if (openingTab > 0) {
            tabNavigationBar.selectTab(openingTab, false);
            openingTab = 0;
        }
        if (owner.sessionsBehindManagers()) {
            rebuild(owner::catchUpSessions);
        }
    }

    @Override
    public boolean pendingChanges() {
        return super.pendingChanges() || owner.sessionsHaveChanges();
    }

    @Override
    public void undo() {
        super.undo();
        if (owner.sessionsHaveChanges()) {
            rebuild(owner::discardSessionChanges);
        }
    }

    @SuppressWarnings("ConstantConditions")
    void rebuild(Runnable change) {
        int tab = Math.max(tabNavigationBar.getTabs().indexOf(tabManager.getCurrentTab()), 0);
        Screen next = new YaclConfigScreen(owner, owner.rebuild(build, change), parent, tab);
        //? if <26.2 {
        minecraft.setScreen(next);
        //?} else {
        /*minecraft.gui.setScreen(next);
        *///?}
    }
}
