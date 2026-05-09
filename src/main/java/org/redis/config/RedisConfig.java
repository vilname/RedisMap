package org.redis.config;

import io.github.cdimascio.dotenv.Dotenv;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConfig {

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private final String host;
    private final int port;
    private final String password;
    private final int timeout;

    public RedisConfig() {
        this.host = getEnv("REDIS_HOST", "localhost");
        this.port = Integer.parseInt(getEnv("REDIS_PORT", "6379"));
        this.password = getEnv("REDIS_PASSWORD", null);
        this.timeout = Integer.parseInt(getEnv("REDIS_TIMEOUT", "2000"));
    }

    private String getEnv(String key, String defaultValue) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        String dotenvValue = dotenv.get(key);
        if (dotenvValue != null && !dotenvValue.isEmpty()) {
            return dotenvValue;
        }

        return defaultValue;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public int getTimeout() {
        return timeout;
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
}
