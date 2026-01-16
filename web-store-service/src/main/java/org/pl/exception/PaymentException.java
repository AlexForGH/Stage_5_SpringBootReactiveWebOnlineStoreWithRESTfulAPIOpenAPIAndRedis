package org.pl.exception;

/**
 * Базовое исключение для ошибок оплаты
 */
public class PaymentException extends RuntimeException {
    private final String userMessage;

    public PaymentException(String message) {
        super(message);
        this.userMessage = message;
    }

    public PaymentException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.userMessage = message;
    }

    public PaymentException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
