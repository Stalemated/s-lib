package com.stalemated.lib.config.internal;

import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.annotation.Format;
import com.stalemated.lib.config.annotation.RangeDouble;
import com.stalemated.lib.config.annotation.RangeFloat;
import com.stalemated.lib.config.annotation.RangeInt;
import com.stalemated.lib.config.annotation.RangeLong;
import com.stalemated.lib.util.internal.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static com.stalemated.lib.util.math.MathUtils.isFinite;

public final class Constraints {

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
            RangeInt.class, Constraints::intRange,
            RangeLong.class, Constraints::longRange,
            RangeFloat.class, Constraints::floatRange,
            RangeDouble.class, Constraints::doubleRange
    );

    private static final Set<Class<?>> FLOATING_TYPES = Set.of(float.class, Float.class, double.class, Double.class);

    private Constraints() {}

    public static Constraint of(String key, Field field, Class<?> type, Type genericType, ConfigNode contentTemplate, DeepCopy deepCopy) {
        List<Class<? extends Annotation>> present = rangesOn(field);
        if (present.size() > 1) {
            throw new IllegalArgumentException("'" + key + "' has more than one range annotation: " + names(present));
        }
        if (present.isEmpty() && field != null && field.isAnnotationPresent(Format.class)) {
            throw new IllegalArgumentException("@Format on '" + key + "' needs a @Range annotation, it only formats sliders");
        }
        if (isContainer(type)) {
            if (!present.isEmpty()) {
                requireRangedContents(key, present.get(0), type, contentTemplate);
            }
            return containerOf(type, genericType, contentTemplate, deepCopy);
        }
        if (present.isEmpty()) return unranged(type, deepCopy);

        Class<? extends Annotation> range = present.get(0);
        if (range != RANGE_BY_TYPE.get(type)) {
            throw new IllegalArgumentException("@" + range.getSimpleName() + " on '" + key + "' does not apply to a " + type.getSimpleName() + " option");
        }
        return READERS.get(range).apply(key, field);
    }

    public static Constraint ofFields(String key, Class<?> type, Map<Field, ConfigNode> children) {
        Map<Field, Constraint> constrained = new LinkedHashMap<>();
        children.forEach((field, child) -> {
            if (!(child.constraint() instanceof Constraint.None)) {
                constrained.put(field, child.constraint());
            }
        });
        if (constrained.isEmpty()) return Constraint.NONE;

        Constraint.Fields fields = new Constraint.Fields(constrained);
        if (fields.clamps() && !ReflectionUtils.hasNoArgsConstructor(type)) {
            throw new IllegalArgumentException(type.getSimpleName() + " in '" + key + "' needs a no-argument constructor to clamp its ranged fields");
        }
        return fields;
    }

    public static boolean isRanged(Field field) {
        return !rangesOn(field).isEmpty();
    }

    private static List<Class<? extends Annotation>> rangesOn(Field field) {
        if (field == null) return List.of();
        return RANGE_ANNOTATIONS.stream().filter(field::isAnnotationPresent).toList();
    }

    private static boolean isContainer(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    }

    private static Constraint unranged(Class<?> type, DeepCopy deepCopy) {
        if (FLOATING_TYPES.contains(type)) return Constraint.FINITE;
        return DeepCopy.copiesAsIs(type) ? Constraint.NONE : new Constraint.Writable(deepCopy, type);
    }

    private static Constraint containerOf(Class<?> type, Type genericType, ConfigNode contentTemplate, DeepCopy deepCopy) {
        boolean isMap = Map.class.isAssignableFrom(type);
        Class<?> firstTypeArgument = typeArgument(genericType, 0);
        Constraint contents = contentTemplate != null
                ? contentTemplate.constraint()
                : unranged(isMap ? typeArgument(genericType, 1) : firstTypeArgument, deepCopy);
        return isMap
                ? new Constraint.MapValues(type, firstTypeArgument, contents)
                : new Constraint.Elements(type, firstTypeArgument, contents);
    }

    private static Class<?> typeArgument(Type genericType, int index) {
        return ReflectionUtils.resolveGenericTypeArgument(genericType, index, Object.class);
    }

    private static void requireRangedContents(String key, Class<? extends Annotation> range, Class<?> type, ConfigNode contentTemplate) {
        String annotation = "@" + range.getSimpleName() + " on '" + key + "'";
        if (contentTemplate == null) {
            throw new IllegalArgumentException(annotation + " needs @Nest to apply to the contents of a " + type.getSimpleName());
        }
        if (!(contentTemplate instanceof OptionInfo)) {
            throw new IllegalArgumentException(annotation + " does not apply to a " + type.getSimpleName() + " of " + contentTemplate.type().getSimpleName());
        }
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
        if (!isFinite(min) || !isFinite(max) || !isFinite(step)) {
            throw new IllegalArgumentException(annotation + " on '" + key + "' needs finite values, got min=" + min + ", max=" + max + ", step=" + step);
        }
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(annotation + " on '" + key + "': min (" + min + ") > max (" + max + ")");
        }
        if (step.compareTo(zero) <= 0) {
            throw new IllegalArgumentException(annotation + " on '" + key + "': step must be greater than 0, got " + step);
        }
    }
}
