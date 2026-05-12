package com.docMind.exception;

public class UserEmailNotExistException extends RuntimeException {
    public UserEmailNotExistException(String message) {
        super(message);
    }
}
