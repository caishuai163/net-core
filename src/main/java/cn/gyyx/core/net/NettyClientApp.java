package cn.gyyx.core.net;

import com.google.protobuf.GeneratedMessage;

public class NettyClientApp {

	public static void main(String[] args) throws Exception {

		GeneratedMessage message = ClientTest.send();

		System.out.println(message.toString());
		long startTime = System.currentTimeMillis();
		for(int i=0; i<1000; i++) {
			message = ClientTest.send();

			System.out.println(message.toString());
		}
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("当前程序耗时："+ (endTime-startTime) + "ms");
		
	}
}
