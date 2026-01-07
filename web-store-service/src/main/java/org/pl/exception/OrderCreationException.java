package org.pl.exception;

/**
 * Исключение при ошибке создания заказа
 */
public class OrderCreationException extends PaymentException {
    public OrderCreationException(String message) {
        super("Ошибка создания заказа: " + message);
    }

    public OrderCreationException(String message, Throwable cause) {
        super("Ошибка создания заказа: " + message, cause);
    }
}