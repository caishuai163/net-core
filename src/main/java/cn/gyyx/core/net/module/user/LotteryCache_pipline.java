package cn.gyyx.core.net.module.user;

import redis.clients.jedis.*;

import java.util.*;

/**
 * Created by DerCg on 2018-05-31.
 */
public class LotteryCache_pipline {
    static JedisPoolConfig config = new JedisPoolConfig();
    static JedisPool pool;


    static {
        //设置最大实例总数
        config.setMaxTotal(10);
        //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
        config.setMaxIdle(5);
        config.setMinIdle(5);
        config.setMaxWaitMillis(1000 * 100);
        pool = new JedisPool(config, "10.14.28.43", 63791, 100000);
//        pool = new JedisPool(config, "192.168.42.133", 6379, 100000);
//        pool = new JedisPool(config, "10.12.54.11", 3000, 100000,"Passw0rd");
    }

    //del lottery:123:receive:1236978
    //del lottery:123:receive:users
//    public static void main(String[] args) throws Exception {
//        long stat = System.currentTimeMillis();
//        int exCount=100000;
//        for (int i = 0; i < exCount; i++) {
//            System.out.println("第：" + i);
//            lottery();
//        }
//        long end = System.currentTimeMillis();
//        System.out.println("have:" + (end - stat));
//    }

    public static String lottery(String actionId, String account){
        Jedis jedis = pool.getResource();
        String redisGiftKey = "gifts."+actionId;//pipeline
        String redisUserCountKey = "lottery:pipeline:user:"+account;
        try {
            // pipeline 获取数据
            Set<String> keys = new HashSet<>();
            keys.add(redisGiftKey);
            keys.add(redisUserCountKey);

            Pipeline p = jedis.pipelined();
            Map<String, byte[]> result = new HashMap<>();
            Map<String, Response<byte[]>> responses = new HashMap<>(keys.size());

            for (String key : keys) {
                responses.put(key, p.get(key.getBytes()));
            	//p.get(key.getBytes());
            }
            
            //List<Object> list = p.syncAndReturnAll();
            
            
           
            p.sync();
            for (String k : responses.keySet()) {
                result.put(k, responses.get(k).get());
            }
            UserLotteryCountOuterClass.UserLotteryCount userLotteryCount = UserLotteryCountOuterClass
                    .UserLotteryCount.parseFrom((result.get(redisUserCountKey)));

//            System.out.println("userLotteryNum:" + userLotteryCount.getLotteryCount());

            GiftsListOuterClass.GiftsList giftListPro = GiftsListOuterClass.GiftsList.parseFrom((result.get(redisGiftKey)));
            Map<String, GiftsListOuterClass.Gift> giftMap = giftListPro.getGiftMapMap();

            if (userLotteryCount.getLotteryCount() <= 0) {
                System.out.println("用户抽奖资格已用完");
                return "error_no_lotteryCount";
            }

            GiftsListOuterClass.Gift defaultLotteryBean = getDefault(giftMap);
            if (defaultLotteryBean == null) {
                System.out.println("未设置默认奖品");
                return "error_no_default_prize";
            }

            List<GiftsListOuterClass.Gift> giftList = new ArrayList<>();
            for (String k : giftMap.keySet()) {
                giftList.add(giftMap.get(k));
            }
            // lottery
            GiftsListOuterClass.Gift resultGift = getLotteryBean(giftList);
            System.out.println("lotteryResult:" + resultGift.getEnglish());

            if (!resultGift.getEnglish().equals("thanks") && resultGift.getNum() <= 0) {
                System.out.println(resultGift.getName() + "奖品已抽完");
                // 默认奖
                resultGift = defaultLotteryBean;
            }
            // pipeline 回写数据
            // set data
            if (!resultGift.getEnglish().equals("thanks")) {
                GiftsListOuterClass.Gift.Builder giftBuilder = GiftsListOuterClass.Gift.newBuilder(resultGift);
                giftBuilder.setNum(resultGift.getNum() - 1);

                GiftsListOuterClass.GiftsList.Builder giftsMapBuilder = GiftsListOuterClass.GiftsList.newBuilder(giftListPro);
                giftsMapBuilder.putGiftMap(resultGift.getEnglish(), giftBuilder.build());
                p.set(redisGiftKey.getBytes(), giftsMapBuilder.build().toByteArray());
            }

            UserLotteryCountOuterClass.UserLotteryCount.Builder returnLotteryCount = UserLotteryCountOuterClass
                    .UserLotteryCount.newBuilder(userLotteryCount);
            returnLotteryCount.setLotteryCount(userLotteryCount.getLotteryCount() - 1);
            p.set(redisUserCountKey.getBytes(), returnLotteryCount.build().toByteArray());

            p.sync();
            return resultGift.getEnglish();

        }catch(Exception e){
        	System.out.println(e);
        	return "error_system";
        }
        finally {
            pool.returnResourceObject(jedis);
        }
    }

    public static GiftsListOuterClass.Gift getDefault(Map<String, GiftsListOuterClass.Gift> map) {
        return map.get("thanks");
    }

    public static GiftsListOuterClass.Gift getLotteryBean(List<GiftsListOuterClass.Gift> list) {
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
            sortOrignalRates.add(tempSumRate.longValue() * 1.0 / sumRate.longValue());
        }

        // 根据区块值来获取抽取到的物品索引
        double nextDouble = Math.random();
        sortOrignalRates.add(nextDouble);
        Collections.sort(sortOrignalRates);

        // 根据物品索引返回物品信息
        return list.get(sortOrignalRates.indexOf(nextDouble));
    }


}
