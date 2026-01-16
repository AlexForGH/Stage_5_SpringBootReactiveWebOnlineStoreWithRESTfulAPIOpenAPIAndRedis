package org.pl.service;

import org.pl.repository.FakeUserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class UserBalanceService {

    private final FakeUserRepository fakeUserRepository;

    public UserBalanceService(FakeUserRepository fakeUserRepository) {
        this.fakeUserRepository = fakeUserRepository;
    }

    public BigDecimal getUserBalance(Long userId) {
        return fakeUserRepository.getUserBalance(userId);
    }

    public void updateUserBalance(Long userId, BigDecimal newBalance) {
        fakeUserRepository.updateUserBalance(userId, newBalance);
    }
}
