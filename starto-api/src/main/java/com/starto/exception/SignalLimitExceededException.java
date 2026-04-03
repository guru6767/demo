// SignalLimitExceededException.java
package com.starto.exception;

public class SignalLimitExceededException extends RuntimeException {
    public SignalLimitExceededException(String message) {
        super(message);
    }
}