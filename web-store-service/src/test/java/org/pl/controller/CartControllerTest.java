package org.pl.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pl.dao.Item;
import org.pl.dao.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pl.controller.Actions.*;

class CartControllerTest extends ControllerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @BeforeEach
    void setUp() {
        // Проверяем, что контроллер зарегистрирован
        assertThat(context.getBean(CartController.class)).isNotNull();
    }

    @Test
    void testCartPageRendering() {
        // Подготовка данных
        Map<Long, Integer> cartItems = Map.of(1L, 2, 3L, 1);
        Item itemMacBook = new Item(
                "MacBook Pro 16\"",
                "img1.jpg",
                new BigDecimal("2499.99"),
                "Laptop"
        );
        itemMacBook.setId(1L);

        Item itemAirPods = new Item(
                "AirPods Pro",
                "img2.jpg",
                new BigDecimal("249.99"),
                "Headphones"
        );
        itemAirPods.setId(3L);

        List<Item> items = List.of(itemMacBook, itemAirPods);
        BigDecimal total = new BigDecimal("5249.97");

        // Настройка моков
        when(sessionItemsCountsService.getCartItems(any())).thenReturn(Mono.just(cartItems));
        when(cartService.getItemsByItemsCounts(any())).thenReturn(Mono.just(items));
        when(cartService.getTotalItemsSum(any())).thenReturn(Mono.just(total));

        // Выполнение запроса и проверки
        webTestClient.get()
                .uri(cartAction)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    // Проверяем, что шаблон содержит ожидаемые данные
                    assertThat(html)
                            .contains("MacBook Pro")
                            .contains("AirPods Pro")
                            .contains("5249.97");

                    // Проверяем наличие форм
                    assertThat(html).contains("<form");
                    assertThat(html).contains("action=\"" + cartAction + "\"");
                    assertThat(html).contains("action=\"" + buyAction + "\"");
                });
    }

    @Test
    void testUpdateItemQuantity() {
        when(sessionItemsCountsService.updateItemCount(any(), eq(5L), eq("PLUS")))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(cartAction)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=5&action=PLUS")
                .exchange()
                .expectHeader().location(cartAction);

        verify(sessionItemsCountsService).updateItemCount(any(), eq(5L), eq("PLUS"));
    }

    @Test
    void testCompleteBuyingItems() {
        Order order = new Order(
                "ORD-2024-100",
                new BigDecimal("2999.99"),
                LocalDateTime.now()
        );
        order.setId(100L);

        when(cartService.createSaveOrders(any())).thenReturn(Mono.just(order));

        webTestClient.post()
                .uri(buyAction)
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().location(ordersAction + "/100");
    }

    @Test
    void testCompleteBuyingItem() {
        Order order = new Order(
                "ORD-2024-101",
                new BigDecimal("1299.99"),
                LocalDateTime.now()
        );
        order.setId(101L);

        when(cartService.createSaveOrder(eq(8L), any()))
                .thenReturn(Mono.just(order));

        webTestClient.post()
                .uri(buyAction + "/8")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().location(ordersAction + "/101");
    }
}