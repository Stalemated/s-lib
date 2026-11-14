package com.stalemated.lib.predicate.target;


import java.util.Optional;

/**
 * Creates {@link TargetMatcher}s from item target expressions.
 * <p>
 * Supports {@code *}, {@code !<expr>}, {@code regex:<pattern>}, {@code <namespace>:*},
 * {@code #<namespace>:<tag>}, exact item ids, and the empty string, which never matches.
 * {@code !} can be nested, for example {@code !!minecraft:stick}.
 */
public final class TargetMatcherFactory {

    private TargetMatcherFactory() {}

    /**
     * Parses an item target expression into a {@link TargetMatcher}.
     *
     * @param target the item target expression; it is trimmed, and {@code null} or empty gives
     *               a matcher that never matches
     * @return the matcher
     * @throws IllegalArgumentException if the syntax, the regex, or the id is invalid
     */
    public static TargetMatcher create(String target) {
        String trimmed = target == null ? "" : target.trim();
        try {
            return parse(trimmed);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid item target '" + trimmed + "': " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to parse {@code target}, returning {@link Optional#empty()} instead of throwing
     * if the expression is invalid.
     *
     * @param target the item target expression, as in {@link #create(String)}
     * @return an {@link Optional} containing the matcher, or empty if invalid
     */
    public static Optional<TargetMatcher> tryCreate(String target) {
        try {
            return Optional.of(create(target));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static TargetMatcher parse(String target) {
        TargetKind kind = TargetKind.of(target);
        String operand = kind.operandOf(target);
        return switch (kind) {
            case NONE -> new NoneStrategy();
            case ALL -> new AllItemsStrategy();
            case NEGATED -> new NotStrategy(parseNegated(operand.trim()));
            case REGEX -> new RegexStrategy(operand);
            case NAMESPACE -> new NamespaceStrategy(operand);
            case TAG -> new TagStrategy(operand);
            case ITEM -> new ItemStrategy(operand);
        };
    }

    private static TargetMatcher parseNegated(String operand) {
        if (operand.isEmpty()) {
            throw new IllegalArgumentException("'!' needs a target to negate");
        }
        return parse(operand);
    }
}
