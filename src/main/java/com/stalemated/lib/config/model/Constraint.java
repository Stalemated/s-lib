package com.stalemated.lib.config.model;

import com.stalemated.lib.util.math.MathUtils;

public sealed interface Constraint permits Constraint.None, Constraint.IntRange, Constraint.LongRange, Constraint.FloatRange, Constraint.DoubleRange {

    Constraint NONE = new None();

    Object apply(Object value);

    record None() implements Constraint {
        @Override
        public Object apply(Object value) {
            return value;
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
                    ? MathUtils.roundToSignificantDigits(MathUtils.clamp(number.floatValue(), min, max))
                    : value;
        }
    }

    record DoubleRange(double min, double max, double step) implements Constraint {
        @Override
        public Object apply(Object value) {
            return value instanceof Number number
                    ? MathUtils.roundToSignificantDigits(MathUtils.clamp(number.doubleValue(), min, max))
                    : value;
        }
    }
}
