package com.pronoia.junit.rule.mllp;

public class MllpJUnitResourceException extends RuntimeException {
    public MllpJUnitResourceException(String message) {
        super(message);
    }

    public MllpJUnitResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MllpJUnitResourceException(Throwable cause) {
        super(cause);
    }

    public MllpJUnitResourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}