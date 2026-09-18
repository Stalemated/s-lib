package com.stalemated.lib.config.network;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.stalemated.lib.config.io.ConfigSerializer;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import net.minecraft.network.PacketByteBuf;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Encapsulates network serialization and deserialization of config data.
 * <p>
 * Guarantees that:
 * <ul>
 *   <li>Only options matching the target {@link SyncMode} are transmitted.</li>
 *   <li>Local-only options ({@link SyncMode#NONE}) and transient fields are never leaked.</li>
 *   <li>Incoming values are strictly clamped against numeric boundary constraints before being applied.</li>
 *   <li>Packets support up to 256KB safely, avoiding Minecraft's default 32KB string limit.</li>
 * </ul>
 */
public final class ConfigNetworkPayload {

    public static final int MAX_PAYLOAD_SIZE = 262144; // 256 KB
    private static final Jankson JANKSON = Jankson.builder().build();

    private ConfigNetworkPayload() {}

    /**
     * Serializes synced options from a config instance into a compact JSON string.
     * <p>
     * To save network bandwidth and reduce parsing complexity, this method does NOT
     * produce a fully nested JSON object (e.g., {@code {"combat": {"attackSpeed": 1.6}}}).
     * Instead, it creates a flat JSON object where keys are the dot-separated paths generated
     * by the {@link OptionTree} (e.g., {@code {"combat.attackSpeed": 1.6}}).
     *
     * @param tree The option schema tree.
     * @param configInstance The source config POJO.
     * @param targetMode The sync mode filter.
     * @return Compact flat JSON representation of the filtered options.
     */
    public static String serialize(OptionTree tree, Object configInstance, SyncMode targetMode) {
        JsonObject jsonObject = new JsonObject();

        for (OptionInfo option : tree.getSyncedOptions()) {
            if (option.getSyncMode() == targetMode) {
                Object val = option.getValue(configInstance);

                if (val != null) {
                    jsonObject.put(option.getKey(), JANKSON.toJson(val));
                }
            }
        }
        return jsonObject.toJson(false, false);
    }

    /**
     * Writes filtered config options into a packet buffer.
     *
     * @param buf The target packet buffer.
     * @param tree The option schema tree.
     * @param configInstance The source config POJO.
     * @param targetMode The sync mode filter.
     */
    public static void write(PacketByteBuf buf, OptionTree tree, Object configInstance, SyncMode targetMode) {
        String json = serialize(tree, configInstance, targetMode);
        
        if (json.length() > MAX_PAYLOAD_SIZE) {
            LOGGER.error("Config payload is too large to sync! Size: {} chars, Max: {} ({}KB). Sync aborted to prevent server disconnect.", json.length(), MAX_PAYLOAD_SIZE, MAX_PAYLOAD_SIZE / 1024);
            // Empty string so the receiver reads and ignores it safely
            buf.writeString("", MAX_PAYLOAD_SIZE);
            return;
        }
        buf.writeString(json, MAX_PAYLOAD_SIZE);
    }

    /**
     * Serializes all server-synced options.
     *
     * @param buf The target packet buffer.
     * @param tree The option schema tree.
     * @param configInstance The source config POJO.
     */
    public static void writeSynced(PacketByteBuf buf, OptionTree tree, Object configInstance) {
        write(buf, tree, configInstance, SyncMode.OVERRIDE_CLIENT);
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
     * @param targetInstance The target config POJO to update.
     * @param serializer The config serializer to parse custom types.
     */
    public static void applyFromJson(String json, OptionTree tree, Object targetInstance, ConfigSerializer<?> serializer) {
        if (json == null || json.trim().isEmpty() || targetInstance == null) {
            return;
        }

        try {
            JsonObject jsonObject = JANKSON.load(json);
            
            for (String key : jsonObject.keySet()) {
                OptionInfo option = tree.get(key);
                
                if (option == null) {
                    LOGGER.debug("Skipping unknown config key received over network: {}", key);
                    continue;
                }

                if (option.getSyncMode() == SyncMode.NONE) {
                    LOGGER.warn("Rejected attempt to remotely modify local-only option: {}", key);
                    continue;
                }

                JsonElement elem = jsonObject.get(key);
                if (elem == null) continue;

                Object rawValue;
                try {
                    rawValue = serializer != null 
                        ? serializer.deserializeType(elem, option.getGenericType())
                        : JANKSON.getMarshaller().marshallCarefully(option.getType(), elem); // fallback
                } catch (Exception e) {
                    LOGGER.warn("Received malformed data for option '{}': {}", key, e.getMessage());
                    continue;
                }
                Object clampedValue = option.clampValue(rawValue);

                option.setValue(targetInstance, clampedValue);
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
     * @param targetInstance The target config POJO to update.
     * @param serializer The config serializer.
     */
    public static void readAndApply(PacketByteBuf buf, OptionTree tree, Object targetInstance, ConfigSerializer<?> serializer) {
        String json = buf.readString(MAX_PAYLOAD_SIZE);
        applyFromJson(json, tree, targetInstance, serializer);
    }
}
