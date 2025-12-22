package org.pl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

@DataR2dbcTest
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1"
})
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findLastOrderNumber_shouldReturnLatestByDate() {
        StepVerifier.create(orderRepository.findLastOrderNumber())
                .expectNext("ORD-2024-005")
                .verifyComplete();
    }
}

