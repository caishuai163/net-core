package cn.gyyx.core.net.module.user;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Throwables;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by DerCg on 2018-05-31.
 */
public class LotteryCache {
    static JedisPoolConfig config = new JedisPoolConfig();

    static JedisPool pool;

    static {
        //设置最大实例总数
        config.setMaxTotal(10);
        //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
        config.setMaxIdle(5);
        config.setMinIdle(5);
        config.setMaxWaitMillis(1000 * 100);
        pool = new JedisPool(config, "192.168.6.126", 6379, 100000);
    }

    public static String lottery(String actionId, String account) {
        Jedis jedis = pool.getResource();
        LuaReadLotteryBean resultGift = null;
        try {
            // read data
            List<String> param = new ArrayList<>();
            param.add(actionId);
            param.add(account);

            String s = (String) jedis.evalsha("259908f14a010adef7a779f238b0f6471819fefb", new ArrayList<String>(), param);

            JSONObject obj = JSONObject.parseObject(s);
            Integer userLotteryNum = obj.getInteger("userLotteryNum");

            JSONArray array = obj.getJSONArray("lotteryInfo");
            List<LuaReadLotteryBean> list = JSONObject.parseArray(array.toJSONString(), LuaReadLotteryBean.class);

            if (userLotteryNum <= 0) {
                return "error_no_lottery_count";
            }

            LuaReadLotteryBean defaultLotteryBean = getDefault(list);
            if (defaultLotteryBean == null) {
                return "error_prize_set";
            }

            // lottery
            resultGift = getLotteryBean(list);
            System.out.println(resultGift.getName());

            if (!resultGift.getType().equals("thanks") && resultGift.getNum() <= 0) {
                // 默认奖
                return resultGift.getName() + " has done";
            }
            List<String> paramResult = new ArrayList<>();
            paramResult.add(actionId);
            paramResult.add(account);
            paramResult.add(resultGift.getEnglish());
            paramResult.add(System.currentTimeMillis() + "");
            // set data
            jedis.evalsha("51055d17dc0f38060769bedd3ec3d4a9cada5cce", new ArrayList<String>(), paramResult);
        	
        	//jedis.get("aaaa");
        	
        	//jedis.set("aaaa", "dddddd");

        } catch (Exception ex) {
            return "error:" + Throwables.getStackTraceAsString(ex);
        } finally {
            pool.returnResourceObject(jedis);
        }
        //return "lottery:" + resultGift.getEnglish();
        return "lottery:thanks";
    }

    public static LuaReadLotteryBean getDefault(List<LuaReadLotteryBean> list) {
        for (LuaReadLotteryBean bean : list) {
            if (bean.getType().equals("thanks")) {
                return bean;
            }
        }
        return null;
    }

    public static LuaReadLotteryBean getLotteryBean(List<LuaReadLotteryBean> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

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
