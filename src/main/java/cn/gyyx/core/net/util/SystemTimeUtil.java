package cn.gyyx.core.net.util;

public class SystemTimeUtil {

	public static int getTimestamp() {
		
		return (int)System.currentTimeMillis()/1000;
	}
}
