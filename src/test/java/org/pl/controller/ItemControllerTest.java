package org.pl.controller;

import org.junit.jupiter.api.Test;
import org.pl.dao.Item;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ItemControllerTest extends ControllerIntegrationTest {

    @Test
    void getItemsSorted_shouldReturnItemsPageWithChunkedPaging() {
        List<Item> items = List.of(
                createItem(1L, "Laptop Pro", "img1.jpg", "999.99"),
                createItem(2L, "Gaming Mouse", "img2.jpg", "89.99"),
                createItem(3L, "Keyboard", "img3.jpg", "129.99"),
                createItem(4L, "Monitor", "img4.jpg", "299.99"),
                createItem(5L, "Webcam", "img5.jpg", "59.99")
        );

        List<List<Item>> chunkedItems = List.of(
                items.subList(0, 3),  // первые 3 товара
                items.subList(3, 5)   // последние 2 товара
        );

        var page = new PageImpl<>(
                chunkedItems,
                PageRequest.of(0, 5),
                items.size()
        );

        when(itemService.getItemsSorted(any(), eq("NO"), eq(null)))
                .thenReturn(Mono.just(page));
        when(sessionItemsCountsService.getCartItems(any()))
                .thenReturn(Mono.just(Map.of(1L, 1, 3L, 2)));
        when(sessionItemsCountsService.checkItemsCount(any()))
                .thenReturn(Mono.just(3));

        webTestClient.get()
                .uri("/items?pageNumber=1&pageSize=5&sort=NO")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    // Проверяем что все товары отображаются
                    assertThat(html).contains("Laptop Pro");
                    assertThat(html).contains("Gaming Mouse");
                    assertThat(html).contains("Keyboard");
                    assertThat(html).contains("Monitor");
                    assertThat(html).contains("Webcam");

                    // Проверяем структуру чанков (возможно rows в таблице)
                    // В шаблоне могут быть строки по 3 товара
                    assertThat(html).containsPattern("(?s).*row.*item.*");

                    // Проверяем пагинацию
                    assertThat(html).contains("page"); // элементы пагинации
                    assertThat(html).contains("1"); // текущая страница
                });
    }

    @Test
    void getItemsSorted_withSearchAndChunking_shouldWorkCorrectly() {
        List<Item> macItems = List.of(
                createItem(1L, "MacBook Pro 16\"", "mac1.jpg", "2499.99"),
                createItem(2L, "MacBook Air M2", "mac2.jpg", "1299.99"),
                createItem(3L, "Mac Mini", "mac3.jpg", "699.99"),
                createItem(4L, "iMac 24\"", "mac4.jpg", "1499.99"),
                createItem(5L, "Mac Studio", "mac5.jpg", "1999.99"),
                createItem(6L, "Mac Pro", "mac6.jpg", "5999.99")
        );

        List<List<Item>> chunkedItems = List.of(
                macItems.subList(0, 3),
                macItems.subList(3, 6)
        );

        var page = new PageImpl<>(
                chunkedItems,
                PageRequest.of(0, 6),
                macItems.size()
        );

        when(itemService.getItemsSorted(any(), eq("PRICE_ASC"), eq("mac"))).thenReturn(Mono.just(page));
        when(sessionItemsCountsService.getCartItems(any())).thenReturn(Mono.just(Map.of()));
        when(sessionItemsCountsService.checkItemsCount(any())).thenReturn(Mono.just(0));

        webTestClient.get()
                .uri("/items?pageNumber=1&pageSize=6&sort=PRICE_ASC&search=mac")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    // Все Mac товары должны быть на странице
                    assertThat(html).contains("MacBook Pro");
                    assertThat(html).contains("MacBook Air");
                    assertThat(html).contains("Mac Mini");
                    assertThat(html).contains("iMac");
                    assertThat(html).contains("Mac Studio");
                    assertThat(html).contains("Mac Pro");

                    // Поисковый запрос сохранен
                    assertThat(html).contains("value=\"mac\"");
                });
    }

    @Test
    void getItemsSorted_withPartialLastChunk_shouldHandleCorrectly() {
        List<Item> items = List.of(
                createItem(1L, "Item 1", "img1.jpg", "100.00"),
                createItem(2L, "Item 2", "img2.jpg", "200.00"),
                createItem(3L, "Item 3", "img3.jpg", "300.00"),
                createItem(4L, "Item 4", "img4.jpg", "400.00"),
                createItem(5L, "Item 5", "img5.jpg", "500.00"),
                createItem(6L, "Item 6", "img6.jpg", "600.00"),
                createItem(7L, "Item 7", "img7.jpg", "700.00")
        );

        // Чанки по 3: [1,2,3], [4,5,6], [7]
        List<List<Item>> chunkedItems = List.of(
                items.subList(0, 3),
                items.subList(3, 6),
                items.subList(6, 7)
        );

        var page = new PageImpl<>(
                chunkedItems,
                PageRequest.of(0, 10),
                items.size()
        );

        when(itemService.getItemsSorted(any(), eq("NO"), eq(null))).thenReturn(Mono.just(page));
        when(sessionItemsCountsService.getCartItems(any())).thenReturn(Mono.just(Map.of()));
        when(sessionItemsCountsService.checkItemsCount(any())).thenReturn(Mono.just(0));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    for (int i = 1; i <= 7; i++) {
                        assertThat(html).contains("Item " + i);
                    }
                });
    }

    @Test
    void getItemsSorted_withSortingPriceDesc_shouldOrderCorrectly() {
        //товары в разном порядке цен
        List<Item> items = List.of(
                createItem(1L, "Cheap Item", "img1.jpg", "50.00"),
                createItem(2L, "Medium Item", "img2.jpg", "150.00"),
                createItem(3L, "Expensive Item", "img3.jpg", "500.00"),
                createItem(4L, "Budget Item", "img4.jpg", "30.00")
        );

        // После сортировки по цене DESC: 500, 150, 50, 30
        List<Item> sortedItems = List.of(
                items.get(2), // 500
                items.get(1), // 150
                items.get(0), // 50
                items.get(3)  // 30
        );

        // Чанки по 3: [500, 150, 50], [30]
        List<List<Item>> chunkedItems = List.of(
                sortedItems.subList(0, 3),
                sortedItems.subList(3, 4)
        );

        var page = new PageImpl<>(
                chunkedItems,
                PageRequest.of(0, 10),
                items.size()
        );

        when(itemService.getItemsSorted(any(), eq("PRICE_DESC"), eq(null))).thenReturn(Mono.just(page));
        when(sessionItemsCountsService.getCartItems(any())).thenReturn(Mono.just(Map.of()));
        when(sessionItemsCountsService.checkItemsCount(any())).thenReturn(Mono.just(0));

        webTestClient.get()
                .uri("/items?sort=PRICE_DESC")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    // Проверяем порядок в HTML (дорогие сначала)
                    int expensiveIndex = html.indexOf("Expensive Item");
                    int mediumIndex = html.indexOf("Medium Item");
                    int cheapIndex = html.indexOf("Cheap Item");
                    int budgetIndex = html.indexOf("Budget Item");

                    // Expensive должен быть раньше всех
                    assertThat(expensiveIndex)
                            .isLessThan(mediumIndex)
                            .isLessThan(cheapIndex)
                            .isLessThan(budgetIndex);
                });
    }

    @Test
    void getItemsSorted_page2_shouldShowSecondPage() {
        //15 товаров, на странице 5, значит 3 страницы
        List<Item> allItems = createTestItems(15); // создаем 15 тестовых товаров

        // Сервис вернет для страницы 2 (pageNumber=1 в Pageable)
        // элементы с 5 по 9 (индексы 5,6,7,8,9)
        List<Item> page2Items = allItems.subList(5, 10);

        // Чанки по 3: [5,6,7], [8,9]
        List<List<Item>> chunkedItems = List.of(
                page2Items.subList(0, 3),
                page2Items.subList(3, 5)
        );

        var page = new PageImpl<>(
                chunkedItems,
                PageRequest.of(1, 5),  // page 1 (вторая страница)
                allItems.size()
        );

        when(itemService.getItemsSorted(any(), eq("NO"), eq(null))).thenReturn(Mono.just(page));
        when(sessionItemsCountsService.getCartItems(any())).thenReturn(Mono.just(Map.of()));
        when(sessionItemsCountsService.checkItemsCount(any())).thenReturn(Mono.just(0));

        webTestClient.get()
                .uri("/items?pageNumber=2&pageSize=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    // Проверяем что отображаются товары со второй страницы
                    // (Item 6, Item 7, Item 8, Item 9, Item 10)
                    assertThat(html).contains("Item 6");
                    assertThat(html).contains("Item 7");
                    assertThat(html).contains("Item 8");
                    assertThat(html).contains("Item 9");
                    assertThat(html).contains("Item 10");
                });
    }

    private Item createItem(Long id, String title, String imgPath, String price) {
        Item item = new Item(
                title,
                imgPath,
                new BigDecimal(price),
                "Description of " + title
        );
        item.setId(id);
        return item;
    }

    private List<Item> createTestItems(int count) {
        List<Item> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add(createItem(
                    (long) i,
                    "Item " + i,
                    "img" + i + ".jpg",
                    String.valueOf(i * 100)
            ));
        }
        return items;
    }
}