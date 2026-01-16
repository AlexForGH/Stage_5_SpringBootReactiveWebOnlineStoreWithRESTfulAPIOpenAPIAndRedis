package org.pl.repository;

import org.pl.dao.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FakeUserRepository {

    private User getUserById(Long userId) {
        User user = User.INSTANCE;
        if (user.getId().equals(userId)) {
            return user;
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public BigDecimal getUserBalance(Long userId) {
        User user = getUserById(userId);
        return user.getBalance();
    }

    public void updateUserBalance(Long userId, BigDecimal newBalance) {
        User user = getUserById(userId);
        user.setBalance(newBalance);
    }
}
