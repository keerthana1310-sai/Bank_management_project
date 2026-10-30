package com.bank.service;

/**
 * Base custom exception for banking domain errors.
 * Provides clear, user-friendly messages for college project demonstration.
 */
public class BankingException extends Exception {
    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
