package com.stalemated.lib.config;

@FunctionalInterface
public interface ConfigMerger<T> {
    void merge(T source, T destination);
}
