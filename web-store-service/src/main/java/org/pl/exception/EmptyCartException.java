package org.pl.exception;

/**
 * Исключение при пустой корзине
 */
public class EmptyCartException extends PaymentException {
    public EmptyCartException() {
        super("Корзина пуста");
    }

    public EmptyCartException(Throwable cause) {
        super("Корзина пуста", cause);
    }
}
