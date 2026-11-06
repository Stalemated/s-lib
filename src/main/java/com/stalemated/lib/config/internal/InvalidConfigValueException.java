package com.stalemated.lib.config.internal;

public final class InvalidConfigValueException extends IllegalArgumentException {

    public InvalidConfigValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
