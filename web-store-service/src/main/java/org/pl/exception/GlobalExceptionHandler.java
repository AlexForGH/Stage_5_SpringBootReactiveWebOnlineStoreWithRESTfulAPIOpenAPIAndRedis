package org.pl.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public Mono<ResponseEntity<String>> handlePaymentException(PaymentException e) {
        return Mono.just(ResponseEntity
                .badRequest()
                .body(e.getUserMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleInsufficientFundsException(InsufficientFundsException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Недостаточно средств");
        response.put("currentBalance", e.getCurrentBalance());
        response.put("requiredAmount", e.getRequiredAmount());
        response.put("missingAmount", e.getMissingAmount());
        response.put("message", e.getMessage());

        return Mono.just(ResponseEntity
                .badRequest()
                .body(response));
    }
}
