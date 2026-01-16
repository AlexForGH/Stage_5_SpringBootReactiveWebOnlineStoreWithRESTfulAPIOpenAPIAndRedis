package org.pl.exception;

/**
 * Исключение при ошибке сервиса баланса
 */
public class BalanceServiceException extends PaymentException {
    public BalanceServiceException(String message) {
        super("Ошибка сервиса баланса: " + message);
    }

    public BalanceServiceException(String message, Throwable cause) {
        super("Ошибка сервиса баланса: " + message, cause);
    }
}