package cn.gyyx.core.net.module.user;

import java.net.URLDecoder;

/**
 * Created by DerCg on 2018-05-31.
 */
public class LuaReadLotteryBean {
    private String name;
    private Long chance;
    private Integer num;
    private String type;
    private String prizeCode;
    private String english;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = getUrlDecoder(name);
    }

    public Long getChance() {
        return chance;
    }

    public void setChance(Long chance) {
        this.chance = chance;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrizeCode() {
        return prizeCode;
    }

    public void setPrizeCode(String prizeCode) {
        this.prizeCode = getUrlDecoder(prizeCode);
    }

    private String getUrlDecoder(String s) {
        try {
            return URLDecoder.decode(s, "utf-8");
        } catch (Exception e) {
            return "";
        }
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    @Override
    public String toString() {
        return "LuaReadLotteryBean{" +
                "name='" + name + '\'' +
                ", chance=" + chance +
                ", num=" + num +
                ", type='" + type + '\'' +
                ", prizeCode='" + prizeCode + '\'' +
                ", english='" + english + '\'' +
                '}';
    }

//            name = redis.call("HGET",gift_key, "name"),
//            chance = redis.call("HGET",gift_key,"chance"),
//            num = redis.call("HGET",gift_key,"num"),
//            english = redis.call("HGET",gift_key,"english"),
//            type = redis.call("HGET",gift_key,"type"),
//            prizeCode = redis.call("HGET",gift_key,"prizecode")
}
