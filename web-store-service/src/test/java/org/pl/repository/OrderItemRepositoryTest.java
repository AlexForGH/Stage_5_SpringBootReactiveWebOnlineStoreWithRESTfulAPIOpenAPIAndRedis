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
class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void findByOrderIdWithAssociations_shouldReturnItemsForOrder() {
        StepVerifier.create(orderItemRepository.findByOrderIdWithAssociations(1L).collectList())
                .expectNextMatches(list -> list.size() == 2 && list.stream().allMatch(oi -> oi.getOrderId().equals(1L)))
                .verifyComplete();
    }

    @Test
    void findAllWithAssociations_shouldReturnAllOrderItems() {
        StepVerifier.create(orderItemRepository.findAllWithAssociations().count())
                .expectNextMatches(count -> count >= 5)
                .verifyComplete();
    }
}

