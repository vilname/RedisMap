package org.redis.service;

import org.junit.jupiter.api.*;
import org.redis.config.RedisConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisMapTest {

    private JedisPool jedisPool;
    private RedisMap redisMap;
    private static final String NAMESPACE = "test";

    @BeforeAll
    void setUp() {
        RedisConfig config = new RedisConfig();
        jedisPool = config.createJedisPool();
        redisMap = new RedisMap(jedisPool, NAMESPACE);
    }

    @BeforeEach
    void cleanUp() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(
                    RedisMap.storageKey(NAMESPACE),
                    RedisMap.storageKey("namespace1"),
                    RedisMap.storageKey("namespace2"));
        }
    }

    @AfterAll
    void tearDown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    @Test
    void testPutAndGet() {
        assertNull(redisMap.put("key1", "value1"));
        assertEquals("value1", redisMap.get("key1"));
        assertEquals(1, redisMap.size());

        assertEquals("value1", redisMap.put("key1", "value2"));
        assertEquals("value2", redisMap.get("key1"));
        assertEquals(1, redisMap.size());
    }

    @Test
    void testGetNonExistentKey() {
        assertNull(redisMap.get("nonexistent"));
    }

    @Test
    void testRemove() {
        redisMap.put("key1", "value1");
        redisMap.put("key2", "value2");

        String removed = redisMap.remove("key1");
        assertEquals("value1", removed);
        assertNull(redisMap.get("key1"));
        assertEquals(1, redisMap.size());

        assertNull(redisMap.remove("nonexistent"));
    }

    @Test
    void testContainsKey() {
        redisMap.put("key1", "value1");
        assertTrue(redisMap.containsKey("key1"));
        assertFalse(redisMap.containsKey("key2"));
    }

    @Test
    void testContainsValue() {
        redisMap.put("key1", "value1");
        redisMap.put("key2", "value2");

        assertTrue(redisMap.containsValue("value1"));
        assertTrue(redisMap.containsValue("value2"));
        assertFalse(redisMap.containsValue("value3"));
    }

    @Test
    void testSize() {
        assertEquals(0, redisMap.size());

        redisMap.put("key1", "value1");
        assertEquals(1, redisMap.size());

        redisMap.put("key2", "value2");
        assertEquals(2, redisMap.size());

        redisMap.remove("key1");
        assertEquals(1, redisMap.size());
    }

    @Test
    void testIsEmpty() {
        assertTrue(redisMap.isEmpty());

        redisMap.put("key1", "value1");
        assertFalse(redisMap.isEmpty());

        redisMap.clear();
        assertTrue(redisMap.isEmpty());
    }

    @Test
    void testClear() {
        redisMap.put("key1", "value1");
        redisMap.put("key2", "value2");
        redisMap.put("key3", "value3");

        assertEquals(3, redisMap.size());
        redisMap.clear();
        assertEquals(0, redisMap.size());
        assertNull(redisMap.get("key1"));
    }

    @Test
    void testPutAll() {
        Map<String, String> entries = new HashMap<>();
        entries.put("key1", "value1");
        entries.put("key2", "value2");
        entries.put("key3", "value3");

        redisMap.putAll(entries);

        assertEquals(3, redisMap.size());
        assertEquals("value1", redisMap.get("key1"));
        assertEquals("value2", redisMap.get("key2"));
        assertEquals("value3", redisMap.get("key3"));
    }

    @Test
    void testKeySet() {
        redisMap.put("key1", "value1");
        redisMap.put("key2", "value2");
        redisMap.put("key3", "value3");

        var keySet = redisMap.keySet();
        assertEquals(3, keySet.size());
        assertTrue(keySet.contains("key1"));
        assertTrue(keySet.contains("key2"));
        assertTrue(keySet.contains("key3"));
    }

    @Test
    void testValues() {
        redisMap.put("key1", "value1");
        redisMap.put("key2", "value2");
        redisMap.put("key3", "value3");

        var values = redisMap.values();
        assertEquals(3, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
        assertTrue(values.contains("value3"));
    }

    @Test
    void testEntrySet() {
        redisMap.put("key1", "value1");
        redisMap.put("key2", "value2");

        var entrySet = redisMap.entrySet();
        assertEquals(2, entrySet.size());

        for (Map.Entry<String, String> entry : entrySet) {
            assertTrue(entry.getKey().equals("key1") || entry.getKey().equals("key2"));
            if (entry.getKey().equals("key1")) {
                assertEquals("value1", entry.getValue());
            } else {
                assertEquals("value2", entry.getValue());
            }
        }
    }

    @Test
    void testMultipleOperations() {
        redisMap.put("a", "1");
        redisMap.put("b", "2");

        assertEquals("1", redisMap.get("a"));
        assertTrue(redisMap.containsKey("b"));
        assertFalse(redisMap.containsKey("c"));

        redisMap.put("c", "3");
        assertEquals(3, redisMap.size());

        redisMap.remove("b");
        assertEquals(2, redisMap.size());
        assertFalse(redisMap.containsKey("b"));

        Map<String, String> additional = new HashMap<>();
        additional.put("d", "4");
        additional.put("e", "5");
        redisMap.putAll(additional);

        assertEquals(4, redisMap.size());
        assertEquals("4", redisMap.get("d"));

        assertTrue(redisMap.containsValue("1"));
        assertTrue(redisMap.containsValue("5"));

        redisMap.clear();
        assertTrue(redisMap.isEmpty());
    }

    @Test
    void testNamespaceIsolation() {
        RedisMap map1 = new RedisMap(jedisPool, "namespace1");
        RedisMap map2 = new RedisMap(jedisPool, "namespace2");

        map1.put("test", "value1");
        map2.put("test", "value2");

        assertEquals("value1", map1.get("test"));
        assertEquals("value2", map2.get("test"));

        assertEquals(1, map1.size());
        assertEquals(1, map2.size());

        map1.clear();
        assertNull(map1.get("test"));
        assertEquals("value2", map2.get("test"));
    }
}
