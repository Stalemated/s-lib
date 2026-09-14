package com.stalemated.lib.config.io;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.Nest;
import com.stalemated.lib.config.io.record.DeserializationResult;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * JSON5 serializer that supports line comments via {@link Comment}, boundary validation via
 * {@link OptionTree} metadata, arbitrary recursive nesting via {@link Nest}, and automatic
 * schema migration when new options are added.
 *
 * @param <T> The config data model class.
 */
public class Json5Serializer<T> {

    private final Class<T> configClass;
    private final OptionTree optionTree;
    private final Jankson jankson;
    private final JsonGrammar grammar;

    public Json5Serializer(Class<T> configClass, OptionTree optionTree) {
        this(configClass, optionTree, null);
    }

    public Json5Serializer(Class<T> configClass, OptionTree optionTree, Consumer<Jankson.Builder> customizer) {
        this.configClass = configClass;
        this.optionTree = optionTree;
        Jankson.Builder builder = Jankson.builder();
        if (customizer != null) {
            customizer.accept(builder);
        }
        this.jankson = builder.build();
        this.grammar = JsonGrammar.builder()
                .withComments(true)
                .printWhitespace(true)
                .build();
    }

    public Jankson getJankson() {
        return jankson;
    }

    /**
     * Serializes the config instance into formatted JSON5 with comments,
     * traversing nested structures recursively.
     *
     * @param instance The config instance to serialize.
     * @return Formatted JSON5 string.
     */
    public String serialize(T instance) {
        JsonElement element = jankson.toJson(instance);
        if (!(element instanceof JsonObject rootObject)) {
            return element.toJson(grammar);
        }

        processCommentsAndIgnores("", rootObject);
        return rootObject.toJson(grammar);
    }

    private void processCommentsAndIgnores(String prefix, JsonObject jsonObject) {
        List<String> keys = new ArrayList<>(jsonObject.keySet());
        
        for (String localKey : keys) {
            String fullKey = prefix.isEmpty() ? localKey : prefix + "." + localKey;
            OptionInfo info = optionTree.get(fullKey);
            
            if (info != null) {
                applyComment(jsonObject, localKey, info);
                continue;
            }
            
            if (isPrefixInOptionTree(fullKey)) {
                processChildNode(jsonObject, localKey, fullKey);
            } else {
                jsonObject.remove(localKey); // Remove orphaned nodes
            }
        }
    }

    private void applyComment(JsonObject jsonObject, String localKey, OptionInfo info) {
        if (info.getComment() != null) {
            jsonObject.setComment(localKey, info.getComment());
        }
    }

    private void processChildNode(JsonObject parentObject, String localKey, String fullKey) {
        JsonElement childElem = parentObject.get(localKey);
        
        if (childElem instanceof JsonObject childObject) {
            processCommentsAndIgnores(fullKey, childObject);

            if (childObject.isEmpty()) {
                parentObject.remove(localKey);
            }
        }
    }

    private boolean isPrefixInOptionTree(String prefix) {
        String searchPrefix = prefix + ".";
        for (OptionInfo option : optionTree.all()) {
            if (option.getKey().startsWith(searchPrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deserializes JSON5 text into a config instance, clamping numeric bounds and
     * detecting schema migrations across all nesting levels based on the OptionTree.
     *
     * @param json5Content The raw JSON5 string content.
     * @param defaultFactory Factory to create the default instance if content is empty or corrupt.
     * @return Result containing the deserialized instance and whether disk resave is required.
     */
    public DeserializationResult<T> deserialize(String json5Content, Supplier<T> defaultFactory) {
        if (json5Content == null || json5Content.trim().isEmpty()) {
            return new DeserializationResult<>(defaultFactory.get(), true);
        }

        JsonObject rootObject;
        try {
            rootObject = jankson.load(json5Content);
        } catch (SyntaxError e) {
            throw new RuntimeException("Syntax error while parsing JSON5 config: " + e.getMessage(), e);
        }

        T instance = jankson.fromJson(rootObject, configClass);
        if (instance == null) instance = defaultFactory.get();

        boolean schemaMigrationNeeded = processClampingAndMigration(rootObject, instance);

        return new DeserializationResult<>(instance, schemaMigrationNeeded);
    }

    private boolean processClampingAndMigration(JsonObject rootObject, Object instance) {
        boolean migrationNeeded = false;

        for (OptionInfo option : optionTree.all()) {
            if (isMissingInJson(rootObject, option.getKey())) {
                option.setValue(instance, option.getDefaultValue());
                migrationNeeded = true;
            } else if (applyClampingIfChanged(option, instance)) {
                migrationNeeded = true;
            }
        }
        
        return migrationNeeded;
    }

    private boolean isMissingInJson(JsonObject rootObject, String optionKey) {
        String[] path = optionKey.split("\\.");
        JsonObject current = rootObject;
        
        for (int i = 0; i < path.length; i++) {
            if (current == null || !current.containsKey(path[i])) {
                return true;
            }

            JsonElement elem = current.get(path[i]);
            if (elem instanceof JsonObject) {
                current = (JsonObject) elem;
            } else if (i < path.length - 1) {
                // Hitting a primitive
                return true;
            }
        }
        return false;
    }

    private boolean applyClampingIfChanged(OptionInfo option, Object instance) {
        Object currentVal = option.getValue(instance);
        Object clamped = option.clampValue(currentVal);

        if (currentVal != null && !currentVal.equals(clamped)) {
            option.setValue(instance, clamped);
            return true;
        }
        return false;
    }
}
