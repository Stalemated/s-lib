package com.stalemated.lib.config.io;

import blue.endless.jankson.*;
import blue.endless.jankson.api.Marshaller;
import blue.endless.jankson.api.SyntaxError;
import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.Nest;
import com.stalemated.lib.config.io.record.DeserializationResult;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
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
    private final ConfigSchemaValidator schemaValidator;
    private final SchemaEnforcer<T> schemaEnforcer;

    public Json5Serializer(Class<T> configClass, OptionTree optionTree) {
        this(configClass, optionTree, null);
    }

    public Json5Serializer(Class<T> configClass, OptionTree optionTree, Consumer<Jankson.Builder> customizer) {
        this.configClass = configClass;
        this.optionTree = optionTree;
        this.schemaValidator = new ConfigSchemaValidator(optionTree);
        
        Jankson.Builder builder = Jankson.builder();
        registerDoubleSerializer(builder);

        if (customizer != null) {
            customizer.accept(builder);
        }
        this.jankson = builder.build();
        this.grammar = JsonGrammar.builder()
                .withComments(true)
                .printWhitespace(true)
                .build();
                
        this.schemaEnforcer = new SchemaEnforcer<>(optionTree, jankson, schemaValidator);
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

        formatAndCleanAst("", rootObject);
        return rootObject.toJson(grammar);
    }

    private void registerDoubleSerializer(Jankson.Builder builder) {
        BiFunction<Float, Marshaller, JsonElement> floatSerializer =
                (f, m) -> new JsonPrimitive(Double.parseDouble(String.valueOf(f)));
        builder.registerSerializer(Float.class, floatSerializer);
        builder.registerSerializer(float.class, floatSerializer);
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
            return new DeserializationResult<>(defaultFactory.get(), true, false);
        }

        JsonObject rootObject;
        try {
            rootObject = jankson.load(json5Content);
        } catch (SyntaxError e) {
            throw new RuntimeException("Syntax error while parsing JSON5 config: " + e.getMessage(), e);
        }

        T instance = jankson.fromJson(rootObject, configClass);
        if (instance == null) instance = defaultFactory.get();

        return schemaEnforcer.enforce(rootObject, instance);
    }
}
