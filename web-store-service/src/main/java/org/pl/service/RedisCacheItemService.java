package org.pl.service;

import org.pl.dao.Item;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RedisCacheItemService {


    private static final Duration DEFAULT_TTL = Duration.ofSeconds(10);
    private final ReactiveValueOperations<String, Object> valueOps;
    private static final String ITEM_KEY_PREFIX = "item:";


    public RedisCacheItemService(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.valueOps = reactiveRedisTemplate.opsForValue();
    }

    public Mono<Item> getItemFromCache(Long id) {
        return valueOps
                .get(buildItemKey(id))
                .cast(Item.class)
                .doOnSuccess(item -> {
                    if (item != null) {
                        System.out.println("getItemFromCache item with id = " + item.getId());
                    }
                }).doOnError(error -> System.out.println("getItemFromCache error: " + error.getMessage()));
    }

    public Mono<Boolean> saveItemToCache(Item item) {
        return saveItemToCache(item, DEFAULT_TTL);
    }

    public Mono<Boolean> saveItemToCache(Item item, Duration ttl) {
        if (item == null || item.getId() == null) {
            return Mono.error(new IllegalArgumentException("Item or item ID cannot be null"));
        }

        return valueOps.set(buildItemKey(item.getId()), item, ttl).doOnSuccess(success -> {
            if (Boolean.TRUE.equals(success)) {
                System.out.println("saveItemToCache TRUE item id = " + item.getId());
            } else {
                System.out.println("saveItemToCache FALSE item id = " + item.getId());
            }
        }).doOnError(error -> System.out.println("saveItemToCache error: " + error.getMessage()));
    }

    private String buildItemKey(Long id) {
        return ITEM_KEY_PREFIX + id;
    }
}