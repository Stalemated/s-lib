package com.stalemated.lib.config.io;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

/**
 * Validates the structure of a parsed JSON config against the expected OptionTree schema.
 * Ensures no orphaned/garbage keys are left in the configuration file.
 */
public class ConfigSchemaValidator {
    private final OptionTree optionTree;

    public ConfigSchemaValidator(OptionTree optionTree) {
        this.optionTree = optionTree;
    }

    /**
     * Checks if the JSON object contains keys that are not mapped in the OptionTree.
     *
     * @param rootObject The parsed JSON object.
     * @return true if orphaned nodes (garbage keys) are detected.
     */
    public boolean containsOrphanedNodes(JsonObject rootObject) {
        if (rootObject == null) return false;
        return scanForOrphans("", rootObject);
    }

    private boolean scanForOrphans(String prefix, JsonObject currentObject) {
        for (String localKey : currentObject.keySet()) {
            String fullKey = prefix.isEmpty() ? localKey : prefix + "." + localKey;

            // If it's a registered exact option (including Maps/Lists), it's valid
            if (optionTree.get(fullKey) != null) continue;

            if (isPrefixRegistered(fullKey)) {
                JsonElement childElem = currentObject.get(localKey);

                if (childElem instanceof JsonObject childObject) {
                    if (scanForOrphans(fullKey, childObject)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given prefix corresponds to a valid nested category in the OptionTree.
     *
     * @param prefix The category path (e.g. "combat.weapons").
     * @return true if there are registered options under this prefix.
     */
    public boolean isPrefixRegistered(String prefix) {
        String searchPrefix = prefix + ".";
        for (OptionInfo option : optionTree.all()) {
            if (option.getKey().startsWith(searchPrefix)) {
                return true;
            }
        }
        return false;
    }
}
