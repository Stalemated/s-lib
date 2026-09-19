package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.google.gson.Gson;
import com.stalemated.lib.config.io.DeserializationResult;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

/**
 * Enforces schema rules defined by the OptionTree onto a parsed JSON object.
 * Responsible for clamping values, assigning defaults to missing keys,
 * detecting silent mutations, and identifying partial corruption.
 */
class Json5SchemaEnforcer<T> {

    private enum ProcessResult {
        OK,
        CLAMPED,
        CORRUPTED
    }

    private final OptionTree optionTree;
    private final Jankson jankson;
    private final Json5SchemaValidator schemaValidator;
    private final Gson gson;

    public Json5SchemaEnforcer(OptionTree optionTree, Jankson jankson, Json5SchemaValidator schemaValidator, Gson gson) {
        this.optionTree = optionTree;
        this.jankson = jankson;
        this.schemaValidator = schemaValidator;
        this.gson = gson;
    }

    /**
     * Applies clamping, default value assignment, and schema validation.
     *
     * @param rootObject The raw JSON5 object parsed by Jankson.
     * @param instance   The Java instance populated by Jankson.
     * @return Result containing the state and whether a save is required.
     */
    public DeserializationResult<T> enforce(JsonObject rootObject, T instance) {
        boolean migrationNeeded = false;
        boolean partialCorruptionDetected = false;

        for (OptionInfo option : optionTree.all()) {
            JsonElement elem = getElementFromJson(rootObject, option.getKey());

            if (elem == null) {
                option.setValue(instance, option.getDefaultValue());
                migrationNeeded = true;
            } else {
                ProcessResult result = processExistingOption(option, elem);

                if (result == ProcessResult.CORRUPTED) {
                    option.setValue(instance, option.getDefaultValue());
                    partialCorruptionDetected = true;
                } else if (result == ProcessResult.CLAMPED) {
                    migrationNeeded = true;
                }
            }
        }

        boolean hasOrphans = schemaValidator.containsOrphanedNodes(rootObject);

        return new DeserializationResult<>(
                instance,
                migrationNeeded || partialCorruptionDetected || hasOrphans,
                partialCorruptionDetected
        );
    }

    private ProcessResult processExistingOption(OptionInfo option, JsonElement elem) {
        try {
            Object parsed = gson.fromJson(elem.toJson(false, false), option.genericType());
            Object clamped = option.clampValue(parsed);

            if (parsed == null || !parsed.equals(clamped)) {
                return ProcessResult.CLAMPED;
            }

            if (isSilentlyMutated(elem, parsed)) {
                return ProcessResult.CLAMPED;
            }

            return ProcessResult.OK;
        } catch (Exception e) {
            return ProcessResult.CORRUPTED;
        }
    }

    private boolean isSilentlyMutated(JsonElement elem, Object parsed) {
        if (!(elem instanceof JsonPrimitive)) return false;

        JsonElement reMarshalled = jankson.toJson(parsed);
        String originalJson = elem.toJson(false, false);
        String newJson = reMarshalled.toJson(false, false);

        return !originalJson.equals(newJson);
    }

    private JsonElement getElementFromJson(JsonObject rootObject, String optionKey) {
        String[] path = optionKey.split("\\.");
        JsonObject current = rootObject;

        for (int i = 0; i < path.length; i++) {
            if (current == null || !current.containsKey(path[i])) {
                return null;
            }

            JsonElement elem = current.get(path[i]);
            if (i == path.length - 1) {
                return elem;
            } else if (elem instanceof JsonObject childObject) {
                current = childObject;
            } else {
                return null; // Path breaks prematurely
            }
        }
        return null;
    }
}
