package cn.gyyx.core.net.module.user;

import redis.clients.jedis.*;

import java.util.*;

/**
 * Created by DerCg on 2018-05-31.
 */
public class LotteryCache_pipline {
    /**
     * 抽奖redis初始化
     */
    static JedisPoolConfig config = new JedisPoolConfig();
    static JedisPool pool;

    static {
        // 设置最大实例总数
        config.setMaxTotal(10);
        // 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
        config.setMaxIdle(5);
        config.setMinIdle(5);
        config.setMaxWaitMillis(1000 * 100);
        pool = new JedisPool(config, "10.14.28.43", 63791, 100000);
        // pool = new JedisPool(config, "192.168.42.133", 6379, 100000);
        // pool = new JedisPool(config, "10.12.54.11", 3000, 100000,"Passw0rd");
    }

    // del lottery:123:receive:1236978
    // del lottery:123:receive:users
    // public static void main(String[] args) throws Exception {
    // long stat = System.currentTimeMillis();
    // int exCount=100000;
    // for (int i = 0; i < exCount; i++) {
    // System.out.println("第：" + i);
    // lottery();
    // }
    // long end = System.currentTimeMillis();
    // System.out.println("have:" + (end - stat));
    // }
    /**
     * 抽奖业务方法
     * 
     * @param actionId
     * @param account
     * @return
     */
    public static String lottery(String actionId, String account) {
        Jedis jedis = pool.getResource();
        /**
         * 奖品的key
         */
        String redisGiftKey = "gifts." + actionId;// pipeline
        /**
         * 用户抽奖次数的key
         */
        String redisUserCountKey = "lottery:pipeline:user:" + account;
        try {
            // pipeline 获取数据
            Set<String> keys = new HashSet<>();
            keys.add(redisGiftKey);
            keys.add(redisUserCountKey);

            Pipeline p = jedis.pipelined();
            Map<String, byte[]> result = new HashMap<>();
            Map<String, Response<byte[]>> responses = new HashMap<>(
                    keys.size());
            /**
             * 获取奖品信息和用户抽奖次数
             */
            for (String key : keys) {
                responses.put(key, p.get(key.getBytes()));
                // p.get(key.getBytes());
            }

            // List<Object> list = p.syncAndReturnAll();

            p.sync();
            for (String k : responses.keySet()) {
                result.put(k, responses.get(k).get());
            }
            /**
             * 用户抽奖次数转成bean
             */
            UserLotteryCountOuterClass.UserLotteryCount userLotteryCount = UserLotteryCountOuterClass.UserLotteryCount
                    .parseFrom((result.get(redisUserCountKey)));

            // System.out.println("userLotteryNum:" +
            // userLotteryCount.getLotteryCount());

            /** 奖品转成bean */
            GiftsListOuterClass.GiftsList giftListPro = GiftsListOuterClass.GiftsList
                    .parseFrom((result.get(redisGiftKey)));
            Map<String, GiftsListOuterClass.Gift> giftMap = giftListPro
                    .getGiftMapMap();
            /**
             * 判断用户抽奖资格
             */
            if (userLotteryCount.getLotteryCount() <= 0) {
                System.out.println("用户抽奖资格已用完");
                return "error_no_lotteryCount";
            }
            /**
             * 获取默认的奖品，一般为消息参与
             */
            GiftsListOuterClass.Gift defaultLotteryBean = getDefault(giftMap);
            if (defaultLotteryBean == null) {
                System.out.println("未设置默认奖品");
                return "error_no_default_prize";
            }

            /**
             * 将奖品信息转成list
             */
            List<GiftsListOuterClass.Gift> giftList = new ArrayList<>();
            for (String k : giftMap.keySet()) {
                giftList.add(giftMap.get(k));
            }
            /**
             * 概率抽奖方法
             */
            // lottery
            GiftsListOuterClass.Gift resultGift = getLotteryBean(giftList);
            System.out.println("lotteryResult:" + resultGift.getEnglish());
            /**
             * 抽奖过程中没有抽到给予谢谢参与
             */
            if (!resultGift.getEnglish().equals("thanks")
                    && resultGift.getNum() <= 0) {
                System.out.println(resultGift.getName() + "奖品已抽完");
                // 默认奖
                resultGift = defaultLotteryBean;
            }
            // pipeline 回写数据
            // set data
            /**
             * 如果不是谢谢参与,更新奖品信息库存,回写到redis
             */
            if (!resultGift.getEnglish().equals("thanks")) {
                GiftsListOuterClass.Gift.Builder giftBuilder = GiftsListOuterClass.Gift
                        .newBuilder(resultGift);
                giftBuilder.setNum(resultGift.getNum() - 1);

                GiftsListOuterClass.GiftsList.Builder giftsMapBuilder = GiftsListOuterClass.GiftsList
                        .newBuilder(giftListPro);
                giftsMapBuilder.putGiftMap(resultGift.getEnglish(),
                    giftBuilder.build());
                p.set(redisGiftKey.getBytes(),
                    giftsMapBuilder.build().toByteArray());
            }
            /**
             * 用户抽奖次数减1
             */
            UserLotteryCountOuterClass.UserLotteryCount.Builder returnLotteryCount = UserLotteryCountOuterClass.UserLotteryCount
                    .newBuilder(userLotteryCount);
            returnLotteryCount
                    .setLotteryCount(userLotteryCount.getLotteryCount() - 1);
            p.set(redisUserCountKey.getBytes(),
                returnLotteryCount.build().toByteArray());

            p.sync();
            return resultGift.getEnglish();

        } catch (Exception e) {
            System.out.println(e);
            return "error_system";
        } finally {
            pool.returnResourceObject(jedis);
        }
    }

    public static GiftsListOuterClass.Gift getDefault(
            Map<String, GiftsListOuterClass.Gift> map) {
        return map.get("thanks");
    }

    /**
     * 活动概率抽奖
     * 
     * @param list
     * @return
     */
    public static GiftsListOuterClass.Gift getLotteryBean(
            List<GiftsListOuterClass.Gift> list) {
        int size = list.size();

        // 计算总概率
        Long sumRate = 0L;
        for (int i = 0; i < list.size(); i++) {
            sumRate = sumRate + list.get(i).getChance();
        }

        // 计算每个物品在总概率的基础下的概率情况
        List<Double> sortOrignalRates = new ArrayList<>(size);
        Long tempSumRate = 0L;
        for (int i = 0; i < list.size(); i++) {
            tempSumRate = tempSumRate + list.get(i).getChance();
            sortOrignalRates
                    .add(tempSumRate.longValue() * 1.0 / sumRate.longValue());
        }

        // 根据区块值来获取抽取到的物品索引
        double nextDouble = Math.random();
        sortOrignalRates.add(nextDouble);
        Collections.sort(sortOrignalRates);

        // 根据物品索引返回物品信息
        return list.get(sortOrignalRates.indexOf(nextDouble));
    }

}
