package com.stalemated.lib.predicate.target.internal;

import java.util.concurrent.atomic.AtomicInteger;

public final class TagReloads {

    private static final AtomicInteger GENERATION = new AtomicInteger();

    private TagReloads() {}

    public static void onTagsReloaded() {
        GENERATION.incrementAndGet();
    }

    public static int generation() {
        return GENERATION.get();
    }
}
