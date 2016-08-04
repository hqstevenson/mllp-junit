package com.pronoia.junit.rule.mllp;


public class MllpJUnitResourceTimeoutException extends MllpJUnitResourceException {
    public MllpJUnitResourceTimeoutException(String message) {
        super(message);
    }

    public MllpJUnitResourceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public MllpJUnitResourceTimeoutException(Throwable cause) {
        super(cause);
    }

    public MllpJUnitResourceTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
