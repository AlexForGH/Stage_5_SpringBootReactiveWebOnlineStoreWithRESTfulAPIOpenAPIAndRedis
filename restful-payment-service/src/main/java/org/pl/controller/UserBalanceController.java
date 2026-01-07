package org.pl.controller;


import org.pl.payment_service.server.payment.model.BalanceResponse;
import org.pl.payment_service.server.payment.model.BalanceUpdateRequest;
import org.pl.service.UserBalanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
class UserBalanceController implements org.pl.payment_service.server.payment.api.DefaultApi {

    private final UserBalanceService userBalanceService;

    public UserBalanceController(UserBalanceService userBalanceService) {
        this.userBalanceService = userBalanceService;
    }

    @Override
    public ResponseEntity<BalanceResponse> getUserBalance(Long userId) {
        BigDecimal balance = userBalanceService.getUserBalance(userId);
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setUserId(userId);
        balanceResponse.setBalance(balance);
        return new ResponseEntity<>(balanceResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<BalanceResponse> updateUserBalance(
            Long userId,
            BalanceUpdateRequest balanceUpdateRequest
    ) {
        BigDecimal balance = userBalanceService.getUserBalance(userId);
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setUserId(userId);
        balanceResponse.setBalance(balance);
        userBalanceService.updateUserBalance(userId, balanceUpdateRequest.getBalance());
        return new ResponseEntity<>(balanceResponse, (HttpStatus.OK));
    }
}