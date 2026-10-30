package com.stalemated.lib.config.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stalemated.lib.config.io.ConfigSerializer;
import com.stalemated.lib.config.model.OptionChange;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Encapsulates network serialization and deserialization of config data.
 * <p>
 * Guarantees that:
 * <ul>
 *   <li>Only options matching the target {@link SyncMode} are transmitted and applied.</li>
 *   <li>Local-only options ({@link SyncMode#NONE}) and transient fields are never leaked.</li>
 *   <li>Incoming values are strictly clamped against numeric boundary constraints before being applied.</li>
 *   <li>Client-bound payloads carry up to 256 KB; server-bound payloads must stay under the vanilla 32 KB limit.</li>
 * </ul>
 */
public final class ConfigNetworkPayload {

    public static final int MAX_PAYLOAD_BYTES = 262144;

    private static final int VANILLA_SERVER_BOUND_LIMIT_BYTES = 32767;
    private static final int CHANNEL_WRAPPER_BYTES = 512;
    public static final int MAX_SERVER_BOUND_BYTES = VANILLA_SERVER_BOUND_LIMIT_BYTES - CHANNEL_WRAPPER_BYTES;

    private ConfigNetworkPayload() {}

    /**
     * Serializes every option of one sync mode into a flat JSON object.
     * <p>
     * To save network bandwidth and reduce parsing complexity, this method does NOT
     * produce a fully nested JSON object (e.g., {@code {"combat": {"attackSpeed": 1.6}}}).
     * Instead, it creates a flat JSON object where keys are the dot-separated paths generated
     * by the {@link OptionTree} (e.g., {@code {"combat.attackSpeed": 1.6}}).
     *
     * @param tree The option schema tree.
     * @param configInstance The source config POJO.
     * @param targetMode The sync mode filter.
     * @param serializer The config serializer used to map individual options.
     * @return Compact flat JSON representation of the filtered options.
     */
    public static String snapshot(OptionTree tree, Object configInstance, SyncMode targetMode, ConfigSerializer<?> serializer) {
        JsonObject payload = new JsonObject();
        for (OptionInfo option : tree.getSyncedOptions()) {
            if (option.getSyncMode() == targetMode) {
                put(payload, option, option.getValue(configInstance), serializer);
            }
        }
        return payload.toString();
    }

    public static String changes(List<OptionChange> changes, ConfigSerializer<?> serializer) {
        JsonObject payload = new JsonObject();
        for (OptionChange change : changes) {
            put(payload, change.option(), change.clampedValue(), serializer);
        }
        return payload.toString();
    }

    public static FriendlyByteBuf encode(String json) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(json, MAX_PAYLOAD_BYTES);
        return buf;
    }

    /**
     * Deserializes a JSON string payload and safely applies clamped values to the target instance.
     * <p>
     * This method expects a flat JSON object where keys are dot-separated paths
     * (e.g., {@code {"combat.attackSpeed": 1.6}}). It reads them directly via the {@link OptionTree}
     * hash map, bypassing expensive recursive JSON parsing.
     *
     * @param json The raw flat JSON payload received.
     * @param tree The option schema tree.
     * @param acceptedMode The only sync mode this payload is allowed to change.
     * @param targetInstance The target config POJO to update.
     * @param serializer The config serializer to parse custom types.
     */
    public static void applyFromJson(String json, OptionTree tree, SyncMode acceptedMode, Object targetInstance, ConfigSerializer<?> serializer) {
        if (json == null || json.trim().isEmpty() || targetInstance == null || serializer == null) {
            return;
        }

        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            for (String key : jsonObject.keySet()) {
                applySingleOption(key, jsonObject.get(key), tree, acceptedMode, targetInstance, serializer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse config sync payload: {}", e.getMessage(), e);
        }
    }

    /**
     * Reads a config payload from a packet buffer and safely applies clamped values.
     *
     * @param buf The incoming packet buffer.
     * @param tree The option schema tree.
     * @param acceptedMode The only sync mode this payload is allowed to change.
     * @param targetInstance The target config POJO to update.
     * @param serializer The config serializer.
     */
    public static void readAndApply(FriendlyByteBuf buf, OptionTree tree, SyncMode acceptedMode, Object targetInstance, ConfigSerializer<?> serializer) {
        applyFromJson(buf.readUtf(MAX_PAYLOAD_BYTES), tree, acceptedMode, targetInstance, serializer);
    }

    private static void put(JsonObject payload, OptionInfo option, Object value, ConfigSerializer<?> serializer) {
        if (value != null) {
            payload.add(option.getKey(), JsonParser.parseString(serializer.serializeOption(value)));
        }
    }

    private static void applySingleOption(String key, JsonElement elem, OptionTree tree, SyncMode acceptedMode, Object targetInstance, ConfigSerializer<?> serializer) {
        OptionInfo option = tree.get(key);

        if (option == null) {
            LOGGER.debug("Skipping unknown config key received over network: {}", key);
            return;
        }

        if (option.getSyncMode() != acceptedMode) {
            LOGGER.warn("Rejected remote change to '{}': it is a {} option and this payload only carries {} options", key, option.getSyncMode(), acceptedMode);
            return;
        }

        if (elem == null || elem.isJsonNull()) return;

        try {
            Object rawValue = serializer.deserializeOption(elem.toString(), option.genericType());
            Object clampedValue = option.clampValue(rawValue);
            option.setValue(targetInstance, clampedValue);
        } catch (Exception e) {
            LOGGER.warn("Received malformed data for option '{}': {}", key, e.getMessage());
        }
    }
}
