package com.stalemated.lib.config.model;

import com.stalemated.lib.config.annotation.RangeDouble;
import com.stalemated.lib.config.annotation.RangeFloat;
import com.stalemated.lib.config.annotation.RangeInt;
import com.stalemated.lib.config.annotation.RangeLong;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

final class RangeConstraints {

    private static final List<Class<? extends Annotation>> RANGE_ANNOTATIONS =
            List.of(RangeInt.class, RangeLong.class, RangeFloat.class, RangeDouble.class);

    private static final Map<Class<?>, Class<? extends Annotation>> RANGE_BY_TYPE = Map.of(
            int.class, RangeInt.class,
            Integer.class, RangeInt.class,
            long.class, RangeLong.class,
            Long.class, RangeLong.class,
            float.class, RangeFloat.class,
            Float.class, RangeFloat.class,
            double.class, RangeDouble.class,
            Double.class, RangeDouble.class
    );

    private static final Map<Class<? extends Annotation>, BiFunction<String, Field, Constraint>> READERS = Map.of(
            RangeInt.class, RangeConstraints::intRange,
            RangeLong.class, RangeConstraints::longRange,
            RangeFloat.class, RangeConstraints::floatRange,
            RangeDouble.class, RangeConstraints::doubleRange
    );

    private RangeConstraints() {}

    static Constraint of(String key, Field field, Class<?> type) {
        List<Class<? extends Annotation>> present = rangesOn(field);
        if (present.isEmpty() || isContainer(type)) return Constraint.NONE;

        if (present.size() > 1) {
            throw new IllegalArgumentException("'" + key + "' has more than one range annotation: " + names(present));
        }
        Class<? extends Annotation> range = present.get(0);
        if (range != RANGE_BY_TYPE.get(type)) {
            throw new IllegalArgumentException("@" + range.getSimpleName() + " on '" + key + "' does not apply to a " + type.getSimpleName() + " option");
        }
        return READERS.get(range).apply(key, field);
    }

    private static List<Class<? extends Annotation>> rangesOn(Field field) {
        if (field == null) return List.of();
        return RANGE_ANNOTATIONS.stream().filter(field::isAnnotationPresent).toList();
    }

    private static boolean isContainer(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    }

    private static String names(List<Class<? extends Annotation>> ranges) {
        return ranges.stream().map(range -> "@" + range.getSimpleName()).toList().toString();
    }

    private static Constraint intRange(String key, Field field) {
        RangeInt range = field.getAnnotation(RangeInt.class);
        requireValid("@RangeInt", key, range.min(), range.max(), range.step(), 0);
        return new Constraint.IntRange(range.min(), range.max(), range.step());
    }

    private static Constraint longRange(String key, Field field) {
        RangeLong range = field.getAnnotation(RangeLong.class);
        requireValid("@RangeLong", key, range.min(), range.max(), range.step(), 0L);
        return new Constraint.LongRange(range.min(), range.max(), range.step());
    }

    private static Constraint floatRange(String key, Field field) {
        RangeFloat range = field.getAnnotation(RangeFloat.class);
        requireValid("@RangeFloat", key, range.min(), range.max(), range.step(), 0f);
        return new Constraint.FloatRange(range.min(), range.max(), range.step());
    }

    private static Constraint doubleRange(String key, Field field) {
        RangeDouble range = field.getAnnotation(RangeDouble.class);
        requireValid("@RangeDouble", key, range.min(), range.max(), range.step(), 0d);
        return new Constraint.DoubleRange(range.min(), range.max(), range.step());
    }

    private static <N extends Number & Comparable<N>> void requireValid(String annotation, String key, N min, N max, N step, N zero) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(annotation + " on '" + key + "': min (" + min + ") > max (" + max + ")");
        }
        if (step.compareTo(zero) <= 0) {
            throw new IllegalArgumentException(annotation + " on '" + key + "': step must be greater than 0, got " + step);
        }
    }
}
