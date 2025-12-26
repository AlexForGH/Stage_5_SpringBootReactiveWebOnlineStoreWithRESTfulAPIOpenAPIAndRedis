package org.pl.service;

import org.pl.dao.Item;
import org.pl.dao.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CartService {
    private static final String ITEM_NOT_FOUND_IN_CART = "Товар не найден в корзине";

    private final ItemService itemService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final SessionItemsCountsService sessionItemsCountsService;

    public CartService(
            ItemService itemService,
            OrderService orderService,
            OrderItemService orderItemService,
            SessionItemsCountsService sessionItemsCountsService
    ) {
        this.itemService = itemService;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.sessionItemsCountsService = sessionItemsCountsService;
    }

    @Transactional(rollbackFor = {RuntimeException.class})
    public Mono<Order> createSaveOrders(ServerWebExchange exchange) {
        System.out.println("Начало создания заказа из всей корзины");

        return getTotalItemsSum(exchange)
                .doOnNext(total -> System.out.println("Общая сумма корзины: " + total))
                .flatMap(orderService::createOrder)
                .doOnNext(order -> System.out.println("Заказ создан, ID: " + order.getId()))
                .flatMap(savedOrder -> saveOrderAndClearCart(savedOrder, exchange));
    }

    @Transactional(rollbackFor = {RuntimeException.class})
    public Mono<Order> createSaveOrder(Long itemId, ServerWebExchange exchange) {
        System.out.println("Начало создания заказа для товара ID: " + itemId);

        return sessionItemsCountsService.getCartItems(exchange)
                .flatMap(cartItems -> processSingleItemOrder(itemId, cartItems, exchange))
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("Товар " + itemId + " не найден в корзине");
                    return Mono.error(new RuntimeException(ITEM_NOT_FOUND_IN_CART));
                }));
    }

    @Transactional(readOnly = true)
    public Mono<List<Item>> getItemsByItemsCounts(ServerWebExchange exchange) {
        System.out.println("Получение деталей товаров из корзины");

        return sessionItemsCountsService.getCartItems(exchange)
                .doOnNext(cartItems -> System.out.println("Найдено товаров в корзине: " + cartItems.size()))
                .flatMap(this::fetchItemsByCart);
    }

    @Transactional(readOnly = true)
    public Mono<BigDecimal> getTotalItemsSum(ServerWebExchange exchange) {
        System.out.println("Вычисление общей суммы корзины");

        return sessionItemsCountsService.getCartItems(exchange)
                .flatMap(this::calculateTotalSum)
                .doOnNext(total -> System.out.println("Итоговая сумма: " + total));
    }

    // ============== ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==============

    /**
     * Сохраняет заказ и очищает корзину
     * Разделение логики для устранения вложенности
     */
    private Mono<Order> saveOrderAndClearCart(Order savedOrder, ServerWebExchange exchange) {
        return sessionItemsCountsService.getCartItems(exchange)
                .flatMap(cartItems -> {
                    System.out.println("Сохранение " + cartItems.size() + " позиций заказа");
                    return orderItemService.saveOrder(savedOrder, cartItems);
                })
                .then(clearCartAndReturnOrder(exchange, savedOrder));
    }

    /**
     * Очищает корзину и возвращает заказ
     * Использует then для последовательного выполнения
     */
    private Mono<Order> clearCartAndReturnOrder(ServerWebExchange exchange, Order savedOrder) {
        return sessionItemsCountsService.clearCartItems(exchange)
                .doOnSuccess(v -> System.out.println("Корзина успешно очищена"))
                .doOnError(e -> System.out.println("Ошибка при очистке корзины: " + e.getMessage()))
                .thenReturn(savedOrder);
    }

    /**
     * Обрабатывает заказ для одного товара
     * Использует filter для проверки условий
     */
    private Mono<Order> processSingleItemOrder(Long itemId, Map<Long, Integer> cartItems,
                                               ServerWebExchange exchange) {
        return Mono.justOrEmpty(cartItems.get(itemId))
                .filter(quantity -> {
                    boolean isValid = quantity > 0;
                    if (!isValid) {
                        System.out.println("Неверное количество товара: " + quantity);
                    }
                    return isValid;
                })
                .flatMap(quantity -> {
                    System.out.println("Обработка товара " + itemId + ", количество: " + quantity);
                    return calculateItemTotal(itemId, quantity)
                            .flatMap(totalAmount -> createAndSaveSingleItemOrder(itemId, quantity, totalAmount, exchange));
                });
    }

    /**
     * Вычисляет стоимость одного товара с учётом количества
     * Использует map для синхронного преобразования
     */
    private Mono<BigDecimal> calculateItemTotal(Long itemId, Integer quantity) {
        return itemService.getPriceById(itemId)
                .doOnNext(price -> System.out.println("Цена товара " + itemId + ": " + price))
                .map(price -> {
                    BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
                    System.out.println("Сумма для товара " + itemId + ": " + total);
                    return total;
                });
    }

    /**
     * Создаёт и сохраняет заказ для одного товара
     * Плоская структура вместо вложенности
     */
    private Mono<Order> createAndSaveSingleItemOrder(Long itemId, Integer quantity,
                                                     BigDecimal totalAmount, ServerWebExchange exchange) {
        return orderService.createOrder(totalAmount)
                .doOnNext(order -> System.out.println("Создан заказ на сумму: " + totalAmount))
                .flatMap(savedOrder -> saveSingleOrderItem(savedOrder, itemId, quantity, exchange));
    }

    /**
     * Сохраняет одну позицию заказа
     * Композиция операций через then
     */
    private Mono<Order> saveSingleOrderItem(Order savedOrder, Long itemId, Integer quantity,
                                            ServerWebExchange exchange) {
        Map<Long, Integer> singleItemCart = Map.of(itemId, quantity);

        return orderItemService.saveOrder(savedOrder, singleItemCart)
                .doOnSuccess(v -> System.out.println("Позиция заказа сохранена"))
                .then(removeItemAndReturnOrder(exchange, itemId, savedOrder));
    }

    /**
     * Удаляет товар из корзины и возвращает заказ
     */
    private Mono<Order> removeItemAndReturnOrder(ServerWebExchange exchange, Long itemId, Order savedOrder) {
        return sessionItemsCountsService.removeItemFromCart(exchange, itemId)
                .doOnSuccess(v -> System.out.println("Товар " + itemId + " удалён из корзины"))
                .thenReturn(savedOrder);
    }

    /**
     * Получает детальную информацию о товарах в корзине
     * Использует flatMap для параллельной загрузки товаров
     */
    private Mono<List<Item>> fetchItemsByCart(Map<Long, Integer> cartItems) {
        return Flux.fromIterable(cartItems.keySet())
                .doOnNext(itemId -> System.out.println("Загрузка товара ID: " + itemId))
                .flatMap(itemService::getItemById)  // параллельная загрузка
                .collectList()
                .doOnNext(items -> System.out.println("Загружено " + items.size() + " товаров"));
    }

    /**
     * Вычисляет общую сумму корзины
     * Использует reduce для агрегации
     */
    private Mono<BigDecimal> calculateTotalSum(Map<Long, Integer> cartItems) {
        return Flux.fromIterable(cartItems.entrySet())
                .flatMap(this::calculateItemSubtotal)
                .reduce(BigDecimal.ZERO, (sum, subtotal) -> {
                    System.out.println("Добавляем к сумме: " + subtotal);
                    return sum.add(subtotal);
                });
    }

    /**
     * Вычисляет стоимость по одному элементу корзины
     */
    private Mono<BigDecimal> calculateItemSubtotal(Map.Entry<Long, Integer> cartEntry) {
        Long itemId = cartEntry.getKey();
        Integer quantity = cartEntry.getValue();

        return itemService.getPriceById(itemId)
                .map(price -> {
                    BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
                    System.out.printf(
                            "Товар %s: %d x %s = %s%n",
                            itemId, quantity, price, subtotal
                    );
                    return subtotal;
                });
    }
}