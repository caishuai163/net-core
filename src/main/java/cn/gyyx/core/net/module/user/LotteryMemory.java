package cn.gyyx.core.net.module.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LotteryMemory {

	static List<LuaReadLotteryBean> list = new ArrayList<>();
    static int userLotteryNum = 60000000;
    static {
        LuaReadLotteryBean item = new LuaReadLotteryBean();
        item.setChance(100L);
        item.setEnglish("dandanji");
        item.setName("蛋蛋鸡");
        item.setNum(50000000);
        item.setPrizeCode("");
        item.setType("dandanji");
        list.add(item);
        LuaReadLotteryBean item1 = new LuaReadLotteryBean();
        item1.setChance(100L);
        item1.setEnglish("thanks");
        item1.setName("谢谢参与");
        item1.setNum(-1);
        item1.setPrizeCode("");
        item1.setType("thanks");
        list.add(item1);
        LuaReadLotteryBean item2 = new LuaReadLotteryBean();
        item2.setChance(100L);
        item2.setEnglish("yinyuanbao500");
        item2.setName("500银元宝");
        item2.setNum(50000000);
        item2.setPrizeCode("visualnosend");
        item2.setType("visualnosend");
        list.add(item2);
        LuaReadLotteryBean item3 = new LuaReadLotteryBean();
        item3.setChance(100L);
        item3.setEnglish("tongqianflag");
        item3.setName("铜钱铭牌");
        item3.setNum(50000000);
        item3.setPrizeCode("");
        item3.setType("visualnosend");
        list.add(item3);
        LuaReadLotteryBean item4 = new LuaReadLotteryBean();
        item4.setChance(100L);
        item4.setEnglish("yinyuanbao200");
        item4.setName("200银元宝");
        item4.setNum(50000000);
        item4.setPrizeCode("");
        item4.setType("visualnosend");
        list.add(item4);
    }

    public static String lottery(String actionId, String account) {
        System.out.println("userLotteryNum:" + userLotteryNum);

        if (userLotteryNum <= 0) {
            System.out.println("用户抽奖资格已用完");
            return "error_no_lottery_count";
        }

        LuaReadLotteryBean defaultLotteryBean = getDefault(list);
        if (defaultLotteryBean == null) {
            return "error_prize_set";
        }

        // lottery
        LuaReadLotteryBean resultGift = getLotteryBean(list);
        System.out.println(resultGift);
        userLotteryNum = userLotteryNum - 1;
        if (!resultGift.getType().equals("thanks") && resultGift.getNum() <= 0) {
            System.out.println(resultGift.getName() + "奖品已抽完");
            // 默认奖
            return "lottery:" + defaultLotteryBean.getEnglish();
        }
        return "lottery:" + resultGift.getEnglish();
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
