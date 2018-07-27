package cn.gyyx.core.net;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

/**
 * <p>
 * operate redis tools (using jedis)
 * </p>
 */
public class JedisClient {

    static JedisPoolConfig config = new JedisPoolConfig();

    static JedisPool pool;
    /**
     * init jedis config and pool
     */
    static {
        // 设置最大实例总数
        config.setMaxTotal(10);
        // 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
        config.setMaxIdle(5);
        config.setMinIdle(5);
        // wait time
        config.setMaxWaitMillis(1000 * 100);
        pool = new JedisPool(config, "192.168.6.126", 6379, 100000);
    }

    public void set(String key, String value) {
        Jedis jedis = null;

        try {
            jedis = pool.getResource();
            Pipeline pipeline = jedis.pipelined();

            for (int i = 0; i < 100000; i++) {
                pipeline.set(key, value);
            }

            pipeline.syncAndReturnAll();
            System.out.println("aaa");
        } finally {
            pool.returnResourceObject(jedis);
        }

    }

    public String set2(String key, String value) {
        Jedis jedis = null;

        try {
            jedis = pool.getResource();

            return jedis.set(key, value);
        } finally {
            pool.returnResourceObject(jedis);
        }

    }
}
