package com.pronoia.junit.rule.mllp;

public class MllpJUnitResourceCorruptFrameException extends MllpJUnitResourceException {
    public MllpJUnitResourceCorruptFrameException(String message) {
        super(message);
    }

    public MllpJUnitResourceCorruptFrameException(String message, Throwable cause) {
        super(message, cause);
    }

    public MllpJUnitResourceCorruptFrameException(Throwable cause) {
        super(cause);
    }

    public MllpJUnitResourceCorruptFrameException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}