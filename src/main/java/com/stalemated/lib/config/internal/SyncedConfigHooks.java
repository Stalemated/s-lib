package com.stalemated.lib.config.internal;

import com.stalemated.lib.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public interface SyncedConfigHooks {

    ConfigManager<?> manager();

    void sendConfigTo(ServerPlayer player);

    void onPlayerLeave(UUID playerId);

    void onClientJoin(boolean singleplayer);

    void onClientDisconnect();

    void unregisterReceivers();
}
