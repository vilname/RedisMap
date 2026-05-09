package org.redis.service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Прослойка между redis и Map, то есть используем все методы интерфейса Map но данные будут храниться в Redis
 * {@link #keySet()}, {@link #values()}, {@link #entrySet()} — неизменяемые снимки.
 */
public class RedisMap implements Map<String, String> {

    private static final String STORAGE_PREFIX = "RedisMap:";

    private final JedisPool jedisPool;
    private final String redisHashKey;

    public RedisMap(JedisPool jedisPool, String namespace) {
        this.jedisPool = jedisPool;
        this.redisHashKey = STORAGE_PREFIX + Objects.requireNonNull(namespace, "namespace");
    }

    static String storageKey(String namespace) {
        return STORAGE_PREFIX + namespace;
    }

    private static boolean unsupportedKey(Object key) {
        return key == null || !(key instanceof String);
    }

    @Override
    public int size() {
        try (Jedis jedis = jedisPool.getResource()) {
            long len = jedis.hlen(redisHashKey);
            return len > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) len;
        }
    }

    @Override
    public boolean isEmpty() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hlen(redisHashKey) == 0;
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (unsupportedKey(key)) {
            return false;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hexists(redisHashKey, (String) key);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        try (Jedis jedis = jedisPool.getResource()) {
            for (String v : jedis.hvals(redisHashKey)) {
                if (Objects.equals(v, value)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public String get(Object key) {
        if (unsupportedKey(key)) {
            return null;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hget(redisHashKey, (String) key);
        }
    }

    @Override
    public String put(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        try (Jedis jedis = jedisPool.getResource()) {
            String previous = jedis.hget(redisHashKey, key);
            jedis.hset(redisHashKey, key, value);
            return previous;
        }
    }

    @Override
    public String remove(Object key) {
        if (unsupportedKey(key)) {
            return null;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            String k = (String) key;
            String previous = jedis.hget(redisHashKey, k);
            jedis.hdel(redisHashKey, k);
            return previous;
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        if (m.isEmpty()) {
            return;
        }
        Map<String, String> copy = new HashMap<>();
        for (Entry<? extends String, ? extends String> e : m.entrySet()) {
            copy.put(
                    Objects.requireNonNull(e.getKey(), "key"),
                    Objects.requireNonNull(e.getValue(), "value"));
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(redisHashKey, copy);
        }
    }

    @Override
    public void clear() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(redisHashKey);
        }
    }

    @Override
    public Set<String> keySet() {
        try (Jedis jedis = jedisPool.getResource()) {
            return Collections.unmodifiableSet(jedis.hkeys(redisHashKey));
        }
    }

    @Override
    public Collection<String> values() {
        try (Jedis jedis = jedisPool.getResource()) {
            return Collections.unmodifiableCollection(jedis.hvals(redisHashKey));
        }
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> snapshot = new HashMap<>(jedis.hgetAll(redisHashKey));
            return Collections.unmodifiableMap(snapshot).entrySet();
        }
    }
}
