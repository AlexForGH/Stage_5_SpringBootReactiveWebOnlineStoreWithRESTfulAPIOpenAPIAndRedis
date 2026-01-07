package org.pl.controller;

import org.pl.exception.*;
import org.pl.service.CartService;
import org.pl.service.SessionItemsCountsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import static org.pl.controller.Actions.*;

@Controller
@RequestMapping()
public class CartController {

    private final CartService cartService;
    private final SessionItemsCountsService sessionItemsCountsService;

    public CartController(
            CartService cartService,
            SessionItemsCountsService sessionItemsCountsService
    ) {
        this.cartService = cartService;
        this.sessionItemsCountsService = sessionItemsCountsService;
    }

    @GetMapping(cartAction)
    public Mono<Rendering> cartAction(ServerWebExchange exchange) {
        return Mono.zip(
                        sessionItemsCountsService
                                .getCartItems(exchange)
                                .doOnNext(
                                        items -> items.entrySet().removeIf(
                                                entry -> entry.getValue() == 0
                                        )
                                ),
                        cartService.getItemsByItemsCounts(exchange),
                        cartService.getTotalItemsSum(exchange),
                        exchange.getSession()
                )
                .map(tuple -> {
                    var cartItems = tuple.getT1();
                    var items = tuple.getT2();
                    var totalItemsSum = tuple.getT3();
                    var session = tuple.getT4();

                    // Получаем toast из сессии
                    String toastMessage = (String) session.getAttributes().get("toastMessage");
                    String toastType = (String) session.getAttributes().get("toastType");

                    // Удаляем из сессии после получения
                    session.getAttributes().remove("toastMessage");
                    session.getAttributes().remove("toastType");

                    return Rendering.view("cart")
                            .modelAttribute("cartItems", cartItems)
                            .modelAttribute("items", items)
                            .modelAttribute("cartAction", cartAction)
                            .modelAttribute("itemsAction", itemsAction)
                            .modelAttribute("buyAction", buyAction)
                            .modelAttribute("totalItemsSum", totalItemsSum)
                            .modelAttribute("toastMessage", toastMessage)
                            .modelAttribute("toastType", toastType)
                            .build();
                });
    }

    @PostMapping(value = cartAction)
    public Mono<String> increaseDecreaseItemsCount(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String idStr = formData.getFirst("id");
                    if (idStr == null || idStr.trim().isEmpty()) {
                        return Mono.error(new IllegalArgumentException("id is required"));
                    }
                    Long id = Long.parseLong(idStr.trim());
                    String action = formData.getFirst("action");
                    return sessionItemsCountsService.updateItemCount(exchange, id, action)
                            .thenReturn("redirect:" + cartAction);
                });
    }

    @PostMapping(value = buyAction)
    public Mono<String> buyItems(ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(session -> {
                    return cartService.createSaveOrders(exchange)
                            .flatMap(savedOrder -> {
                                session.getAttributes().put("toastMessage",
                                        "Заказ №" + savedOrder.getOrderNumber() + " успешно оформлен!");
                                session.getAttributes().put("toastType", "success");
                                return Mono.just("redirect:" + ordersAction + "/" + savedOrder.getId());
                            })
                            .onErrorResume(e -> {
                                checkErrors(session, e);

                                System.err.println("Ошибка при оформлении заказа: " + e.getMessage());
                                e.printStackTrace();

                                return Mono.just("redirect:" + cartAction);
                            });
                });
    }

    @PostMapping(value = buyAction + "/{id}")
    public Mono<String> buyItem(
            @PathVariable Long id,
            ServerWebExchange exchange
    ) {
        return exchange.getSession()
                .flatMap(session -> {
                    return cartService.createSaveOrder(id, exchange)
                            .flatMap(savedOrder -> {
                                session.getAttributes().put("toastMessage",
                                        "Заказ №" + savedOrder.getOrderNumber() + " успешно оформлен!");
                                session.getAttributes().put("toastType", "success");
                                return Mono.just("redirect:" + ordersAction + "/" + savedOrder.getId());
                            })
                            .onErrorResume(e -> {
                                checkErrors(session, e);

                                System.err.println("Ошибка при оформлении заказа: " + e.getMessage());
                                e.printStackTrace();

                                return Mono.just("redirect:" + itemsAction + "/" + id);
                            });
                });
    }

    private void checkErrors(WebSession session, Throwable e) {
        String userMessage;
        String toastType = "error";

        switch (e) {
            case InsufficientFundsException ife -> userMessage = String.format(
                    "Недостаточно средств. Баланс: %s, требуется: %s",
                    ife.getCurrentBalance(),
                    ife.getRequiredAmount()
            );
            case EmptyCartException emptyCartException -> {
                userMessage = "Корзина пуста";
                toastType = "warning";
            }
            case BalanceServiceException balanceServiceException ->
                    userMessage = "Ошибка сервиса баланса. Попробуйте позже.";
            case OrderCreationException orderCreationException ->
                    userMessage = "Ошибка при создании заказа. Попробуйте позже.";
            case PaymentException paymentException -> userMessage = e.getMessage();
            case null, default -> userMessage = "Произошла непредвиденная ошибка. Попробуйте позже.";
        }

        session.getAttributes().put("toastMessage", userMessage);
        session.getAttributes().put("toastType", toastType);
    }
}
