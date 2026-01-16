package org.pl.exception;

import java.math.BigDecimal;

/**
 * Исключение при недостатке средств
 */
public class InsufficientFundsException extends PaymentException {
    private final BigDecimal currentBalance;
    private final BigDecimal requiredAmount;

    public InsufficientFundsException(BigDecimal currentBalance, BigDecimal requiredAmount) {
        super(String.format("Недостаточно средств. Баланс: %s, требуется: %s",
                currentBalance, requiredAmount));
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public InsufficientFundsException(BigDecimal currentBalance, BigDecimal requiredAmount, Throwable cause) {
        super(String.format("Недостаточно средств. Баланс: %s, требуется: %s",
                currentBalance, requiredAmount), cause);
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }

    public BigDecimal getMissingAmount() {
        return requiredAmount.subtract(currentBalance);
    }
}