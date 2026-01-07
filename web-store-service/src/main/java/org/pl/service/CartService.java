package org.pl.service;

import org.pl.dao.Item;
import org.pl.dao.Order;
import org.pl.exception.EmptyCartException;
import org.pl.exception.InsufficientFundsException;
import org.pl.exception.OrderCreationException;
import org.pl.exception.PaymentException;
import org.pl.webstore.client.payment.api.DefaultApi;
import org.pl.webstore.client.payment.model.BalanceResponse;
import org.pl.webstore.client.payment.model.BalanceUpdateRequest;
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
    private static final String NEGATIVE_QUANTITY = "Количество товара не может быть отрицательным";

    private final ItemService itemService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final SessionItemsCountsService sessionItemsCountsService;
    private final DefaultApi defaultApi;

    public CartService(
            ItemService itemService,
            OrderService orderService,
            OrderItemService orderItemService,
            SessionItemsCountsService sessionItemsCountsService,
            DefaultApi defaultApi
    ) {
        this.itemService = itemService;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.sessionItemsCountsService = sessionItemsCountsService;
        this.defaultApi = defaultApi;
    }

    @Transactional(rollbackFor = {PaymentException.class, RuntimeException.class})
    public Mono<Order> createSaveOrders(ServerWebExchange exchange) {
        System.out.println("Начало создания заказа из всей корзины");

        return sessionItemsCountsService.getCartItems(exchange)
                .doOnNext(cartItems ->
                        System.out.println("Товаров в корзине: " + cartItems.size())
                )
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        System.out.println("Корзина пуста, невозможно создать заказ");
                        return Mono.error(new EmptyCartException());
                    }

                    System.out.println("Корзина не пуста, продолжаем обработку");
                    return processFullCartOrder(cartItems, exchange);
                })
                .doOnSuccess(order ->
                        System.out.println("Заказ успешно создан, ID: " + order.getId() )
                )
                .doOnError(e -> {
                    if (e instanceof PaymentException) {
                        System.err.println("Ошибка оплаты: " + e.getMessage() );
                    } else {
                        System.err.println("Неожиданная ошибка при создании заказа: " + e.getMessage());
                    }
                });
    }

    @Transactional(rollbackFor = {RuntimeException.class})
    public Mono<Order> createSaveOrder(Long itemId, ServerWebExchange exchange) {
        System.out.println("=== Начало создания заказа для товара ID: " + itemId + " ===");

        return sessionItemsCountsService.getCartItems(exchange)
                .doOnNext(cartItems ->
                        System.out.println("Товаров в корзине: " + cartItems.size())
                )
                .flatMap(cartItems -> {
                    if (!cartItems.containsKey(itemId)) {
                        return Mono.error(new RuntimeException(ITEM_NOT_FOUND_IN_CART));
                    }

                    Integer quantity = cartItems.get(itemId);
                    if (quantity <= 0) {
                        return Mono.error(new RuntimeException(NEGATIVE_QUANTITY));
                    }

                    return processSingleItemOrder(itemId, quantity, exchange);
                })
                .doOnSuccess(order ->
                        System.out.println("Заказ для товара успешно создан, ID: " + order.getId())
                )
                .doOnError(e ->
                        System.err.println("Ошибка при создании заказа для товара: " + e.getMessage())
                );
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
     * Запрашивает баланс пользователя у внешнего сервиса
     */
    private Mono<BigDecimal> getUserBalance() {
        return defaultApi.getUserBalance(1L)
                .map(balanceResponse -> {
                    System.out.println("Текущий баланс пользователя " + balanceResponse.getBalance());
                    return balanceResponse.getBalance();
                });
    }

    /**
     * Обновляет баланс пользователя после покупки
     */
    private Mono<BalanceResponse> updateBalanceAfterPurchase(BigDecimal purchaseAmount) {
        return getUserBalance()
                .flatMap(currentBalance -> {
                    BigDecimal newBalance = currentBalance.subtract(purchaseAmount);

                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(new RuntimeException(
                                "Попытка установить отрицательный баланс: " + newBalance
                        ));
                    }

                    BalanceUpdateRequest balanceUpdateRequest = new BalanceUpdateRequest();
                    balanceUpdateRequest.setBalance(newBalance);

                    System.out.println("Обновление баланса: " + currentBalance +
                            " -> " + newBalance + " (списано: " + purchaseAmount + ")");

                    return defaultApi.updateUserBalance(1L, balanceUpdateRequest)
                            .doOnSuccess(response ->
                                    System.out.println("Баланс успешно обновлен: " + response.getBalance())
                            )
                            .doOnError(e ->
                                    System.err.println("Ошибка при обновлении баланса: " + e.getMessage())
                            );
                });
    }

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
     * Обработка заказа для всей корзины
     */
    private Mono<Order> processFullCartOrder(Map<Long, Integer> cartItems, ServerWebExchange exchange) {
        return Mono.zip(
                        getUserBalance().cache(),
                        calculateTotalSum(cartItems).cache()
                )
                .doOnNext(tuple -> {
                    BigDecimal balance = tuple.getT1();
                    BigDecimal total = tuple.getT2();
                    System.out.println("Проверка баланса: " + balance + " >= " + total + " ?");
                })
                .flatMap(tuple -> {
                    BigDecimal userBalance = tuple.getT1();
                    BigDecimal totalAmount = tuple.getT2();

                    // Проверка баланса с использованием кастомного исключения
                    if (userBalance.compareTo(totalAmount) < 0) {
                        return Mono.error(new InsufficientFundsException(userBalance, totalAmount));
                    }

                    System.out.println("Баланс достаточен, создаем заказ...");
                    return orderService.createOrder(totalAmount);
                })
                .flatMap(savedOrder -> {
                    System.out.println("Заказ создан в базе, ID: " + savedOrder.getId());

                    // Сохраняем позиции заказа
                    return orderItemService.saveOrder(savedOrder, cartItems)
                            .doOnSuccess(v ->
                                    System.out.println("Позиции заказа сохранены")
                            )
                            .then(Mono.defer(() -> {
                                // Обновляем баланс
                                return updateBalanceAfterPurchase(savedOrder.getTotalAmount())
                                        .doOnSuccess(response ->
                                                System.out.println("Баланс обновлен: " + response.getBalance())
                                        )
                                        .doOnError(e -> {
                                            System.err.println("Ошибка при обновлении баланса: " + e.getMessage());
                                            // Логируем, но не прерываем процесс
                                        });
                            }))
                            .then(Mono.defer(() -> {
                                // Очищаем корзину
                                return sessionItemsCountsService.clearCartItems(exchange)
                                        .doOnSuccess(v ->
                                                System.out.println("Корзина очищена")
                                        );
                            }))
                            .thenReturn(savedOrder);
                })
                .onErrorMap(e -> {
                    // Преобразуем стандартные исключения в PaymentException
                    if (e instanceof InsufficientFundsException) {
                        return e; // Уже наше исключение
                    } else if (e instanceof RuntimeException && !(e instanceof PaymentException)) {
                        return new OrderCreationException(e.getMessage(), e);
                    }
                    return e;
                });
    }

    /**
     * Обработка заказа для одного товара
     */
    private Mono<Order> processSingleItemOrder(Long itemId, Integer quantity, ServerWebExchange exchange) {
        return Mono.zip(
                        getUserBalance().cache(),
                        calculateItemTotal(itemId, quantity).cache()
                )
                .doOnNext(tuple -> {
                    BigDecimal balance = tuple.getT1();
                    BigDecimal itemTotal = tuple.getT2();
                    System.out.println("Проверка баланса для товара " + itemId + ": " + balance + " >= " + itemTotal + " ?");
                })
                .flatMap(tuple -> {
                    BigDecimal userBalance = tuple.getT1();
                    BigDecimal itemTotal = tuple.getT2();

                    if (userBalance.compareTo(itemTotal) < 0) {
                        return Mono.error(new InsufficientFundsException(userBalance, itemTotal));
                    }

                    System.out.println("Баланс достаточен, создаем заказ для товара...");
                    return orderService.createOrder(itemTotal);
                })
                .flatMap(savedOrder -> {
                    System.out.println("Заказ создан, ID: " + savedOrder.getId());

                    Map<Long, Integer> singleItemCart = Map.of(itemId, quantity);

                    // Сохраняем позицию заказа
                    return orderItemService.saveOrder(savedOrder, singleItemCart)
                            .doOnSuccess(v ->
                                    System.out.println("Позиция заказа сохранена")
                            )
                            .then(Mono.defer(() -> {
                                // Обновляем баланс
                                return updateBalanceAfterPurchase(savedOrder.getTotalAmount())
                                        .doOnSuccess(response ->
                                                System.out.println("Баланс обновлен: " + response.getBalance())
                                        );
                            }))
                            .then(Mono.defer(() -> {
                                // Удаляем товар из корзины
                                return sessionItemsCountsService.removeItemFromCart(exchange, itemId)
                                        .doOnSuccess(v ->
                                                System.out.println("Товар удален из корзины")
                                        );
                            }))
                            .thenReturn(savedOrder);
                })
                .onErrorMap(e -> {
                    if (e instanceof InsufficientFundsException) {
                        return e;
                    } else if (e instanceof RuntimeException && !(e instanceof PaymentException)) {
                        return new OrderCreationException(e.getMessage(), e);
                    }
                    return e;
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
    private Mono<Order> createAndSaveSingleItemOrder(
            Long itemId,
            Integer quantity,
            BigDecimal totalAmount,
            ServerWebExchange exchange
    ) {
        return orderService.createOrder(totalAmount)
                .doOnNext(order -> System.out.println("Создан заказ на сумму: " + totalAmount))
                .flatMap(savedOrder -> saveSingleOrderItem(savedOrder, itemId, quantity, exchange));
    }

    /**
     * Сохраняет одну позицию заказа
     * Композиция операций через then
     */
    private Mono<Order> saveSingleOrderItem(
            Order savedOrder,
            Long itemId,
            Integer quantity,
            ServerWebExchange exchange
    ) {
        Map<Long, Integer> singleItemCart = Map.of(itemId, quantity);

        return orderItemService.saveOrder(savedOrder, singleItemCart)
                .doOnSuccess(v -> System.out.println("Позиция заказа сохранена"))
                .then(removeItemAndReturnOrder(exchange, itemId, savedOrder));
    }

    /**
     * Удаляет товар из корзины и возвращает заказ
     */
    private Mono<Order> removeItemAndReturnOrder(
            ServerWebExchange exchange,
            Long itemId, Order savedOrder
    ) {
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