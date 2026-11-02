package com.stalemated.lib.config.model;

import com.stalemated.lib.util.math.MathUtils;
import com.stalemated.lib.util.reflection.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public sealed interface Constraint permits Constraint.None, Constraint.Finite, Constraint.IntRange, Constraint.LongRange,
        Constraint.FloatRange, Constraint.DoubleRange, Constraint.Elements, Constraint.MapValues, Constraint.Fields {

    Constraint NONE = new None();
    Constraint FINITE = new Finite();

    Object apply(Object value);

    default boolean clamps() {
        return true;
    }

    record None() implements Constraint {
        @Override
        public Object apply(Object value) {
            return value;
        }

        @Override
        public boolean clamps() {
            return false;
        }
    }

    record Finite() implements Constraint {
        @Override
        public Object apply(Object value) {
            if (value instanceof Number number && !MathUtils.isFinite(number)) {
                throw new IllegalArgumentException("must be a finite number, got " + value);
            }
            return value;
        }

        @Override
        public boolean clamps() {
            return false;
        }
    }

    record IntRange(int min, int max, int step) implements Constraint {
        @Override
        public Object apply(Object value) {
            return value instanceof Number number ? MathUtils.clamp(number.intValue(), min, max) : value;
        }
    }

    record LongRange(long min, long max, long step) implements Constraint {
        @Override
        public Object apply(Object value) {
            return value instanceof Number number ? MathUtils.clamp(number.longValue(), min, max) : value;
        }
    }

    record FloatRange(float min, float max, float step) implements Constraint {
        @Override
        public Object apply(Object value) {
            return value instanceof Number number
                    ? MathUtils.roundToSignificantDigits(MathUtils.clamp(requireNumber(number).floatValue(), min, max))
                    : value;
        }
    }

    record DoubleRange(double min, double max, double step) implements Constraint {
        @Override
        public Object apply(Object value) {
            return value instanceof Number number
                    ? MathUtils.roundToSignificantDigits(MathUtils.clamp(requireNumber(number).doubleValue(), min, max))
                    : value;
        }
    }

    record Elements(Class<?> type, Class<?> elementType, Constraint each) implements Constraint {
        @Override
        public Object apply(Object value) {
            if (!(value instanceof Collection<?> source)) return value;
            if (each instanceof None && type.isInstance(source)) return source;

            List<Object> clamped = new ArrayList<>(source.size());
            boolean changed = !type.isInstance(source);
            for (Object element : source) {
                Object result = each.apply(element);
                changed |= !Objects.equals(element, result);
                clamped.add(result);
            }
            if (!changed) return source;

            Collection<Object> copy = ContainerFactory.newCollection(type, elementType);
            copy.addAll(clamped);
            return copy;
        }

        @Override
        public boolean clamps() {
            return each.clamps();
        }
    }

    record MapValues(Class<?> type, Class<?> keyType, Constraint each) implements Constraint {
        @Override
        public Object apply(Object value) {
            if (!(value instanceof Map<?, ?> source)) return value;
            if (each instanceof None && type.isInstance(source)) return source;

            Map<Object, Object> clamped = new LinkedHashMap<>();
            boolean changed = !type.isInstance(source);
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                Object result = each.apply(entry.getValue());
                changed |= !Objects.equals(entry.getValue(), result);
                clamped.put(entry.getKey(), result);
            }
            if (!changed) return source;

            Map<Object, Object> copy = ContainerFactory.newMap(type, keyType);
            copy.putAll(clamped);
            return copy;
        }

        @Override
        public boolean clamps() {
            return each.clamps();
        }
    }

    record Fields(Map<Field, Constraint> byField) implements Constraint {
        public Fields {
            byField = Collections.unmodifiableMap(new LinkedHashMap<>(byField));
        }

        @Override
        public Object apply(Object value) {
            if (value == null) return null;

            Map<Field, Object> clamped = new LinkedHashMap<>();
            byField.forEach((field, constraint) -> {
                Object current = ReflectionUtils.getFieldValue(field, value);
                Object result = constraint.apply(current);
                if (!Objects.equals(current, result)) {
                    clamped.put(field, result);
                }
            });
            if (clamped.isEmpty()) return value;

            Object copy = ReflectionUtils.shallowCopy(value);
            clamped.forEach((field, result) -> ReflectionUtils.setFieldValue(field, copy, result));
            return copy;
        }

        @Override
        public boolean clamps() {
            return byField.values().stream().anyMatch(Constraint::clamps);
        }
    }

    private static Number requireNumber(Number number) {
        if (Double.isNaN(number.doubleValue())) {
            throw new IllegalArgumentException("must be a number, got NaN");
        }
        return number;
    }
}
