package com.filizes.backpacklimit.database.exception;

public class DatabaseInitializationException extends RuntimeException {
    public DatabaseInitializationException(String message) {
        super(message);
    }

    public DatabaseInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}