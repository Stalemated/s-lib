package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.config.Subscription;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.screens.Screen;

final class YaclConfigScreen extends YACLScreen {
    private final YaclScreenBuilder owner;
    private final YaclScreenBuilder.ScreenBuild build;
    private final Screen parent;
    private int openingTab;
    private Subscription viewChanges;

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
        throw new IllegalStateException("Adding or removing nested entries needs the screen from YaclScreenBuilder.generateScreen(), got " + screen.getClass().getName());
    }

    @Override
    protected void init() {
        super.init();
        if (openingTab > 0) {
            tabNavigationBar.selectTab(openingTab, false);
            openingTab = 0;
        }
        followServerView();
    }

    @Override
    public void tick() {
        super.tick();
        if (viewChanges == null && isShown()) {
            followServerView();
        }
    }

    private void followServerView() {
        if (viewChanges == null) {
            viewChanges = owner.onViewChange(this::onServerViewChange);
        }
        if (owner.sessionsBehindManagers() || owner.overridePermissionsChangedSince(build)) {
            rebuild(owner::catchUpSessions);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (viewChanges == null) return;
        viewChanges.unsubscribe();
        viewChanges = null;
    }

    private void onServerViewChange() {
        if (isShown()) {
            rebuild(owner::catchUpSessions);
        }
    }

    @Override
    public boolean pendingChanges() {
        return super.pendingChanges() || owner.sessionsHaveChanges();
    }

    @Override
    public void cancelOrReset() {
        if (pendingChanges()) {
            super.cancelOrReset();
            return;
        }
        YaclScreenBuilder.requestDefaultsOfAvailableOptions(config);
    }

    @Override
    public void undo() {
        super.undo();
        if (owner.sessionsHaveChanges()) {
            rebuild(owner::discardSessionChanges);
        }
    }

    @Override
    public void finishOrSave() {
        boolean saving = pendingChanges();
        super.finishOrSave();
        if (saving && isShown()) {
            rebuild(owner::catchUpSessions);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private boolean isShown() {
        //? if <26.2 {
        return minecraft.screen == this;
        //?} else {
        /*return minecraft.gui.screen() == this;
        *///?}
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
