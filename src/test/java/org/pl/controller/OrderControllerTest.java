package org.pl.controller;

import org.junit.jupiter.api.Test;
import org.pl.dao.Item;
import org.pl.dao.Order;
import org.pl.dto.ItemInOrderDTO;
import org.pl.dto.OrderWithItemsDTO;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class OrderControllerTest extends ControllerIntegrationTest {

    private OrderWithItemsDTO sampleDto(Long id, String number) {
        Order order = new Order(number, BigDecimal.TEN, LocalDateTime.now());
        order.setId(id);
        Item item = new Item("Keyboard", "img", BigDecimal.ONE, "desc");
        item.setId(11L);
        return new OrderWithItemsDTO(order, List.of(new ItemInOrderDTO(item, 1)));
    }

    @Test
    void getOrders_shouldReturnOrdersPage() {
        var dtos = List.of(
                sampleDto(1L, "ORD-2024-001"),
                sampleDto(2L, "ORD-2024-002")
        );

        when(orderItemService.getOrdersWithItems()).thenReturn(Mono.just(dtos));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    // Проверяем что страница содержит данные заказов
                    assertThat(html)
                            .contains("ORD-2024-001")
                            .contains("ORD-2024-002")
                            .contains("Keyboard");
                    assertThat(html).contains("orders");
                });
    }

    @Test
    void getOrderById_shouldReturnOrderPage() {
        var dto = sampleDto(5L, "ORD-2024-005");

        when(orderItemService.getOrderWithItems(5L)).thenReturn(Mono.just(dto));

        webTestClient.get()
                .uri("/orders/5")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    // Проверяем данные заказа
                    assertThat(html).contains("ORD-2024-005");
                    assertThat(html).contains("Keyboard");
                    assertThat(html).contains("order");
                });
    }

    @Test
    void getOrderById_whenOrderNotFound_shouldReturnError() {
        when(orderItemService.getOrderWithItems(anyLong())).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/orders/99")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Internal Server Error");
                });
    }

    @Test
    void getOrderById_withInvalidId_shouldReturnBadRequest() {
        webTestClient.get()
                .uri("/orders/abc")
                .exchange()
                .expectStatus().isBadRequest();
    }
}