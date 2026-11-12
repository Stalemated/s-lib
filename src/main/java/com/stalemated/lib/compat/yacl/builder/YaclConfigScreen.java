package com.stalemated.lib.compat.yacl.builder;

import com.mojang.blaze3d.platform.Window;
import com.stalemated.lib.config.Subscription;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.screens.Screen;

final class YaclConfigScreen extends YACLScreen {
    private final YaclScreenBuilder owner;
    private final YaclScreenBuilder.ScreenBuild build;
    private final Screen parent;
    private YaclScreenView openingView;
    private Subscription viewChanges;
    private Tab buttonsShownFor;

    YaclConfigScreen(YaclScreenBuilder owner, YaclScreenBuilder.ScreenBuild build, Screen parent, YaclScreenView openingView) {
        super(build.lib(), parent);
        this.owner = owner;
        this.build = build;
        this.parent = parent;
        this.openingView = openingView;
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
        if (openingView != null) {
            if (openingView.tab() > 0) {
                tabNavigationBar.selectTab(openingView.tab(), false);
            }
            openingView.restore(this);
            openingView = null;
        }
        followServerView();
    }

    @Override
    public void tick() {
        if (isShown()) followWindowSize();
        showPendingStateOnCurrentTab();
        super.tick();
        if (viewChanges == null && isShown()) {
            followServerView();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void followWindowSize() {
        Window window = minecraft.getWindow();
        int windowWidth = window.getGuiScaledWidth();
        int windowHeight = window.getGuiScaledHeight();
        if (width == windowWidth && height == windowHeight) return;
        //? if >=1.21.11 {
        /*resize(windowWidth, windowHeight);
        *///?} else {
        resize(minecraft, windowWidth, windowHeight);
        //?}
    }

    private void showPendingStateOnCurrentTab() {
        Tab current = tabManager.getCurrentTab();
        if (current == buttonsShownFor) return;
        buttonsShownFor = current;
        if (current instanceof CategoryTab categoryTab) {
            categoryTab.updateButtons();
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
        YaclScreenView view = YaclScreenView.of(this);
        Screen next = new YaclConfigScreen(owner, owner.rebuild(build, change), parent, view);
        //? if <26.2 {
        minecraft.setScreen(next);
        //?} else {
        /*minecraft.gui.setScreen(next);
        *///?}
    }
}
