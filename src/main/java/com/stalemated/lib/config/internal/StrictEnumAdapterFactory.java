package com.stalemated.lib.config.internal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

final class StrictEnumAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) return null;

        Class<?> enumType = rawType.isEnum() ? rawType : rawType.getSuperclass();
        return new StrictEnumAdapter<>(gson.getDelegateAdapter(this, type), enumType);
    }

    private static final class StrictEnumAdapter<T> extends TypeAdapter<T> {

        private final TypeAdapter<T> constants;
        private final Class<?> enumType;

        StrictEnumAdapter(TypeAdapter<T> constants, Class<?> enumType) {
            this.constants = constants;
            this.enumType = enumType;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            constants.write(out, value);
        }

        @Override
        public T read(JsonReader in) throws IOException {
            JsonElement json = JsonParser.parseReader(in);
            T constant = constants.fromJsonTree(json);
            if (constant == null && !json.isJsonNull()) {
                throw new JsonParseException(json + " is not a constant of " + enumType.getSimpleName());
            }
            return constant;
        }
    }
}
