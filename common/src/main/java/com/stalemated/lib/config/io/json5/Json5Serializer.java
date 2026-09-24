package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.Nest;
import com.stalemated.lib.config.io.ConfigSerializer;
import com.stalemated.lib.config.io.DeserializationResult;
import com.stalemated.lib.config.validation.ConfigValidator;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Pure JSON5 serializer that supports line comments via {@link Comment},
 * arbitrary recursive nesting via {@link Nest}, and delegates strict
 * validation/auto-migration to internal enforcers.
 *
 * @param <T> The config data model class.
 */
public class Json5Serializer<T> implements ConfigSerializer<T> {

    private final Class<T> configClass;
    private final OptionTree optionTree;
    private final Jankson jankson;
    private final JsonGrammar grammar;
    private final Json5SchemaValidator schemaValidator;
    private final Gson gson;

    public Json5Serializer(Class<T> configClass, OptionTree optionTree) {
        this(configClass, optionTree, null);
    }

    public Json5Serializer(
            Class<T> configClass,
            OptionTree optionTree,
            Consumer<GsonBuilder> gsonCustomizer
    ) {
        this.configClass = configClass;
        this.optionTree = optionTree;
        this.schemaValidator = new Json5SchemaValidator(optionTree);

        // Jankson used as preprocessor
        this.jankson = Jankson.builder().build();

        GsonBuilder gBuilder = new GsonBuilder();
        SLibGsonDefaults.apply(gBuilder);
        if (gsonCustomizer != null) {
            gsonCustomizer.accept(gBuilder);
        }
        this.gson = gBuilder.create();

        this.grammar = JsonGrammar.builder()
                .withComments(true)
                .printWhitespace(true)
                .build();
    }

    /**
     * Serializes the config instance into formatted JSON5 with comments,
     * traversing nested structures recursively.
     *
     * @param instance The config instance to serialize.
     * @return Formatted JSON5 string.
     */
    @Override
    public String serialize(T instance) {
        String standardJson = gson.toJson(instance);

        JsonObject rootObject;
        try {
            rootObject = jankson.load(standardJson);
        } catch (SyntaxError e) {
            throw new RuntimeException("Unexpected syntax error from internal GSON output: " + e.getMessage(), e);
        }

        formatAndCleanAst("", rootObject);
        Json5CommentInjector.inject(rootObject, optionTree.getSchemaRoot());
        return rootObject.toJson(grammar);
    }

    /**
     * Deserializes JSON5 text into a config instance, clamping numeric bounds and
     * detecting schema migrations across all nesting levels based on the OptionTree.
     *
     * @param json5Content The raw JSON5 string content.
     * @param defaultFactory Factory to create the default instance if content is empty or corrupt.
     * @return Result containing the deserialized instance and whether disk resave is required.
     */
    @Override
    public DeserializationResult<T> deserialize(String json5Content, Supplier<T> defaultFactory) {
        if (json5Content == null || json5Content.trim().isEmpty()) {
            return new DeserializationResult<>(defaultFactory.get(), true, false);
        }

        JsonObject rootObject;
        try {
            rootObject = jankson.load(json5Content);
        } catch (SyntaxError e) {
            throw new RuntimeException("Syntax error while parsing JSON5 config: " + e.getMessage(), e);
        }

        T instance = gson.fromJson(rootObject.toJson(false, false), configClass);
        if (instance == null) instance = defaultFactory.get();

        boolean requiresSave = ConfigValidator.validate(instance, optionTree.getSchemaRoot());

        if (!requiresSave) {
            requiresSave = schemaValidator.containsOrphanedNodes(rootObject);
        }

        if (!requiresSave) {
            requiresSave = hasMissingKeys(rootObject);
        }

        return new DeserializationResult<>(instance, requiresSave, false);
    }

    @Override
    public String serializeOption(Object value) {
        return gson.toJson(value);
    }

    @Override
    public Object deserializeOption(String rawJson, Type targetType) {
        return gson.fromJson(rawJson, targetType);
    }

    private boolean hasMissingKeys(JsonObject rootObject) {
        for (OptionInfo option : optionTree.all()) {
            if (getElementFromJson(rootObject, option.getKey()) == null) {
                return true;
            }
        }
        return false;
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
                return null;
            }
        }
        return null;
    }

    private void formatAndCleanAst(String prefix, JsonObject jsonObject) {
        List<String> keys = new ArrayList<>(jsonObject.keySet());

        for (String localKey : keys) {
            String fullKey = prefix.isEmpty() ? localKey : prefix + "." + localKey;
            OptionInfo info = optionTree.get(fullKey);

            if (info != null) {
                applyComment(jsonObject, localKey, info);
                continue;
            }

            if (schemaValidator.isPrefixRegistered(fullKey)) {
                cleanChildNode(jsonObject, localKey, fullKey);
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

    private void cleanChildNode(JsonObject parentObject, String localKey, String fullKey) {
        JsonElement childElem = parentObject.get(localKey);

        if (childElem instanceof JsonObject childObject) {
            formatAndCleanAst(fullKey, childObject);

            if (childObject.isEmpty()) {
                parentObject.remove(localKey);
            }
        }
    }
}
