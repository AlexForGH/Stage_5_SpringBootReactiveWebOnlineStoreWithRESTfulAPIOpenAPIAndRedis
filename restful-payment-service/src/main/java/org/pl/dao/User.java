package org.pl.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

// этот синглтон для имитации пользователя с рандомным балансом
public enum User {

    INSTANCE;

    private final Long id;
    private BigDecimal balance;

    User() {
        this.id = 1L;
        Random random = new Random();
        this.balance = BigDecimal
                .valueOf(random.nextDouble() * 100_000)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}