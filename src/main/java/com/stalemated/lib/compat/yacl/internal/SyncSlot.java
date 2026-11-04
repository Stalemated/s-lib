package com.stalemated.lib.compat.yacl.internal;

import java.util.Objects;

public record SyncSlot(String container, String entry, String field) {

    public SyncSlot {
        Objects.requireNonNull(container, "container cannot be null");
    }

    public static SyncSlot of(String container) {
        return new SyncSlot(container, null, null);
    }

    public SyncSlot withEntry(String entry) {
        return new SyncSlot(container, entry, field);
    }

    public SyncSlot withField(String field) {
        return new SyncSlot(container, entry, field);
    }

    public boolean overlaps(SyncSlot other) {
        return container.equals(other.container)
                && sameOrUnset(entry, other.entry)
                && sameOrUnset(field, other.field);
    }

    private static boolean sameOrUnset(String first, String second) {
        return first == null || second == null || first.equals(second);
    }
}
