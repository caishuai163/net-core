package cn.gyyx.core.net.util;

import java.util.concurrent.atomic.AtomicLong;

public class GuidUtil {

	private AtomicLong curGuid = new AtomicLong(0);
	
	public long generic() {
		
		return curGuid.incrementAndGet();
	}
}
