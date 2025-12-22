package org.pl.controller;

import org.pl.service.CartService;
import org.pl.service.ItemService;
import org.pl.service.OrderItemService;
import org.pl.service.SessionItemsCountsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
public abstract class ControllerIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;

    @MockitoBean
    protected CartService cartService;

    @MockitoBean
    protected SessionItemsCountsService sessionItemsCountsService;

    @MockitoBean
    protected ItemService itemService;

    @MockitoBean
    protected OrderItemService orderItemService;
}