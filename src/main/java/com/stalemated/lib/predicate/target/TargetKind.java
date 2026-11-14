package com.stalemated.lib.predicate.target;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Syntax classification for item target expressions parsed by {@link TargetMatcherFactory}.
 */
public enum TargetKind {
    /** Empty target; matches nothing. */
    NONE(String::isEmpty, target -> target),
    /** Wildcard {@code *}; matches every stack, empty ones too. */
    ALL("*"::equals, target -> target),
    /** Negated target {@code !<expr>}; inverts the inner matcher. */
    NEGATED(target -> target.startsWith("!"), target -> target.substring(1)),
    /**
     * Regular expression {@code regex:<pattern>}; the pattern must match the whole item id
     * ({@code ns:path}). A pattern that can start with {@code #} is also tested against the
     * stack's tags as {@code #ns:tag}.
     */
    REGEX(target -> target.startsWith("regex:"), target -> target.substring("regex:".length())),
    /** Namespace wildcard {@code <namespace>:*}; matches any item in that namespace. */
    NAMESPACE(target -> target.endsWith(":*"), target -> target.substring(0, target.length() - ":*".length())),
    /** Item tag {@code #<namespace>:<tag>}. */
    TAG(target -> target.startsWith("#"), target -> target.substring(1)),
    /**
     * Any other text: an exact item id {@code <namespace>:<item>}. Without a namespace it uses
     * {@code minecraft}.
     */
    ITEM(target -> true, target -> target);

    private final Predicate<String> syntax;
    private final UnaryOperator<String> operand;

    TargetKind(Predicate<String> syntax, UnaryOperator<String> operand) {
        this.syntax = syntax;
        this.operand = operand;
    }

    /**
     * Returns the kind of {@code target}. The constants are tested in declaration order and
     * the first match wins, so {@code #c:*} is {@link #NAMESPACE}. The text is not trimmed.
     *
     * @param target the item target expression
     * @return the kind, never {@code null}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public static TargetKind of(String target) {
        for (TargetKind kind : values()) {
            if (kind.syntax.test(target)) return kind;
        }
        return ITEM;
    }

    /**
     * Extracts the operand portion of {@code target} by stripping this kind's prefix or suffix.
     * It does not check that {@code target} is of this kind and does not trim it.
     *
     * @param target the item target expression
     * @return the extracted operand
     * @throws StringIndexOutOfBoundsException if {@code target} is shorter than the prefix or
     *                                         suffix, for example {@code REGEX.operandOf("")}
     */
    public String operandOf(String target) {
        return operand.apply(target);
    }
}
