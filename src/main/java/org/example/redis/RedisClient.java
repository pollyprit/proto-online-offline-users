package org.example.redis;

import redis.clients.jedis.Jedis;

import java.util.List;

public class RedisClient {
    // Redis: docker run -d --name redis-stack -p 6379:6379 -p 8001:8001 redis/redis-stack:latest
    private final String REDIS_SERVER = "192.168.2.189";
    private final int REDIS_PORT = 6379;
    private Jedis jedis;

    public RedisClient() {
        jedis = new Jedis(REDIS_SERVER, REDIS_PORT);
    }

    public void setKey(int id, String lastHb, int ttlSec) {
        String key = "id:" + id;
        String value = "last_hb:" + lastHb;

        String ret = jedis.setex(key, ttlSec, value);
        System.out.println("redis: set [" + key + ", "  + value + "], result " + ret);
    }

    // Batch get: "mget id:5 id:6 id:7"
    public List<String> getKeysInBatch(String[] keys) {
        return jedis.mget(keys);
    }

    public void close() {
        jedis.close();
    }
}
