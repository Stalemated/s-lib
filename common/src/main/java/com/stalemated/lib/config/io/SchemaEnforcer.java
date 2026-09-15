package com.stalemated.lib.config.io;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.DeserializationException;
import com.stalemated.lib.config.io.record.DeserializationResult;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

/**
 * Enforces schema rules defined by the OptionTree onto a parsed JSON object.
 * Responsible for clamping values, assigning defaults to missing keys,
 * detecting silent mutations, and identifying partial corruption.
 */
public class SchemaEnforcer<T> {

    private final OptionTree optionTree;
    private final Jankson jankson;
    private final ConfigSchemaValidator schemaValidator;

    public SchemaEnforcer(OptionTree optionTree, Jankson jankson, ConfigSchemaValidator schemaValidator) {
        this.optionTree = optionTree;
        this.jankson = jankson;
        this.schemaValidator = schemaValidator;
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
                ProcessResult result = processExistingOption(instance, option, elem);

                if (result == ProcessResult.CORRUPTED) {
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

    private ProcessResult processExistingOption(Object instance, OptionInfo option, JsonElement elem) {
        try {
            Object parsed = jankson.getMarshaller().marshallCarefully(option.getType(), elem);
            Object clamped = option.clampValue(parsed);
            option.setValue(instance, clamped);

            if (parsed == null || !parsed.equals(clamped)) {
                return ProcessResult.CLAMPED;
            }

            if (isSilentlyMutated(elem, parsed)) {
                return ProcessResult.CLAMPED;
            }

            return ProcessResult.OK;
        } catch (DeserializationException e) {
            option.setValue(instance, option.getDefaultValue());
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
