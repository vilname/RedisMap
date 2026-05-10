package org.redis.config;

import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;

public class RedisConfig {

    private static final Map<String, Object> YAML_REDIS = loadRedisSection();

    private final String host;
    private final int port;
    private final String password;
    private final int timeout;

    public RedisConfig() {
        this.host = getString("REDIS_HOST", "host", "localhost");
        this.port = getInt("REDIS_PORT", "port", 6379);
        this.password = getPassword();
        this.timeout = getInt("REDIS_TIMEOUT", "timeout", 2000);
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    public JedisPool createJedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);

        if (hasPassword()) {
            return new JedisPool(poolConfig, host, port, timeout, password);
        }

        return new JedisPool(poolConfig, host, port, timeout);
    }

    @Override
    public String toString() {
        return "RedisConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", hasPassword=" + hasPassword() +
                ", timeout=" + timeout +
                '}';
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadRedisSection() {
        try (InputStream is = RedisConfig.class.getClassLoader().getResourceAsStream("application.yaml")) {
            if (is == null) {
                throw new FileNotFoundException("application.yaml not found");
            }
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);

            if (root == null) {
                return Collections.emptyMap();
            }

            Object redis = root.get("redis");
            if (redis instanceof Map<?, ?> m) {
                return (Map<String, Object>) m;
            }

            return Collections.emptyMap();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getString(String envKey, String yamlKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        Object yamlVal = YAML_REDIS.get(yamlKey);
        if (yamlVal == null) {
            return defaultValue;
        }

        if (yamlVal instanceof String s) {
            return s.isEmpty() ? defaultValue : s;
        }

        return String.valueOf(yamlVal);
    }

    private static int getInt(String envKey, String yamlKey, int defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return Integer.parseInt(envValue);
        }

        Object yamlVal = YAML_REDIS.get(yamlKey);
        if (yamlVal instanceof Number n) {
            return n.intValue();
        }

        if (yamlVal instanceof String s && !s.isEmpty()) {
            return Integer.parseInt(s);
        }

        return defaultValue;
    }

    private static String getPassword() {
        String envValue = System.getenv("REDIS_PASSWORD");
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        Object yamlVal = YAML_REDIS.get("password");
        if (yamlVal == null) {
            return null;
        }

        String s = yamlVal instanceof String ? (String) yamlVal : String.valueOf(yamlVal);

        return s.isEmpty() ? null : s;
    }
}
