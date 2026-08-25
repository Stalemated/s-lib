package com.stalemated.lib.network;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractNetworkHelper implements NetworkHelper {
    public static final Map<Identifier, ServerReceiver> SERVER_RECEIVERS = new HashMap<>();

    @Override
    public void registerServerReceiver(Identifier id, ServerReceiver receiver) {
        SERVER_RECEIVERS.put(id, receiver);
    }
}
