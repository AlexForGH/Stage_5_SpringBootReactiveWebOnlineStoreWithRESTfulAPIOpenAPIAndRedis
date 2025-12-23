package org.pl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataR2dbcTest
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1"
})
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findByTitleContainingIgnoreCase_shouldFindAllMacBookProItems() {
        StepVerifier.create(itemRepository.findByTitleContainingIgnoreCase("MacBook Pro"))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByTitleContainingIgnoreCase_shouldFindSpecificMacBookPro() {
        StepVerifier.create(itemRepository.findByTitleContainingIgnoreCase("MacBook Pro"))
                .assertNext(item -> {
                    assertThat(item.getTitle()).contains("MacBook Pro");
                    assertThat(item.getPrice()).isGreaterThan(BigDecimal.valueOf(0));
                })
                .assertNext(item -> {
                    assertThat(item.getTitle()).contains("MacBook Pro");
                    assertThat(item.getPrice()).isGreaterThan(BigDecimal.valueOf(0));
                })
                .verifyComplete();
    }
}

