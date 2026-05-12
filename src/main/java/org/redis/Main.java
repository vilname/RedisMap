package org.redis;

import org.redis.config.RedisConfig;
import org.redis.service.RedisMap;
import redis.clients.jedis.JedisPool;

public class Main {
    public static void main(String[] args) {
        RedisConfig config = new RedisConfig();
        System.out.println("Connecting to Redis with config: " + config);

        JedisPool jedisPool = config.createJedisPool();

        // Создание экземпляра RedisMap
        RedisMap redisMap = new RedisMap(jedisPool, "test");

        System.out.println("Redis Map Demo");
        System.out.println("==============");

        // put операции
        redisMap.put("name", "John Doe");
        redisMap.put("email", "john@example.com");
        redisMap.put("city", "Moscow");

        System.out.println("After put operations:");
        System.out.println("Size: " + redisMap.size());
        System.out.println("Get name: " + redisMap.get("name"));

        // containsKey и containsValue
        System.out.println("\nContains key 'email': " + redisMap.containsKey("email"));
        System.out.println("Contains value 'Moscow': " + redisMap.containsValue("Moscow"));

        // keySet и values
        System.out.println("\nKey set: " + redisMap.keySet());
        System.out.println("Values: " + redisMap.values());

        // remove
        String removed = redisMap.remove("city");
        System.out.println("\nRemoved city: " + removed);
        System.out.println("Size after remove: " + redisMap.size());

        // putAll
        java.util.Map<String, String> newEntries = new java.util.HashMap<>();
        newEntries.put("phone", "123-456-789");
        newEntries.put("age", "30");
        redisMap.putAll(newEntries);

        System.out.println("\nAfter putAll:");
        System.out.println("All entries: " + redisMap.entrySet());

        // clear
        redisMap.clear();
        System.out.println("\nAfter clear, is empty: " + redisMap.isEmpty());

        // Закрытие пула соединений
        jedisPool.close();
    }
}