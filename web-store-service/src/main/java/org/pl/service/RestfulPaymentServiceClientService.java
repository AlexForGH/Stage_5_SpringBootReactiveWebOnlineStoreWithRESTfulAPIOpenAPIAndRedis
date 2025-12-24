package org.pl.service;

import org.pl.webstore.client.payment.api.DefaultApi;
import org.pl.webstore.client.payment.model.BalanceResponse;
import org.pl.webstore.client.payment.model.BalanceUpdateRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class RestfulPaymentServiceClientService {

    private final DefaultApi balanceApi;

    public RestfulPaymentServiceClientService(DefaultApi balanceApi) {
        this.balanceApi = balanceApi;
    }

    public Mono<BigDecimal> getBalance(Long userId) {
        return balanceApi.getUserBalance(userId).map(BalanceResponse::getBalance);
    }

    public Mono<BalanceResponse> updateBalance(Long userId, BigDecimal balance) {
        BalanceUpdateRequest request = new BalanceUpdateRequest();
        request.setBalance(balance);
        return balanceApi.updateUserBalance(userId, request);
    }
}