package cn.gyyx.core.net;

import cn.gyyx.core.net.module.user.LotteryCache;
import cn.gyyx.core.net.module.user.LotteryCache_pipline;
import cn.gyyx.core.net.service.EndPoint;

public class JedisTest {

	public static void main(String[] args) {

		//LotteryCache.lottery("123", "1236978");
		//JedisClient client = new JedisClient();
		//client.set2("aaa", "bbb");
		long startTime = System.currentTimeMillis();
		
		//for(int i=0;i<10000;i++) {
			//LotteryCache.lottery("123", "1236978");
			//client.set2("aaa", "bbb");
		//}
		EndPoint point = new EndPoint("",0);
		
		setVal(point);
		setVal2(point);
		System.out.println(point.getIp());
		System.out.println(point.getPort());

		long endTime = System.currentTimeMillis();
		
		System.out.println("当前程序耗时："+ (endTime-startTime) + "ms");
	}
	
	public static void setVal() {
		EndPoint point = new EndPoint("",0);
		setVal(point);
		setVal2(point);
	}
	
	public static void setVal(EndPoint point) {
		point.setIp("192.168.6.21");
		
	}
	
	public static void setVal2(EndPoint point) {
		point.setPort(90);
	}
}
