package cn.gyyx.core.net.mgr;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class EventGroupMgr {

	private EventLoopGroup bossGroup = new NioEventLoopGroup(1,
			(Runnable r) -> new Thread(r,"ACCEPT_THREAD"));
	
	private EventLoopGroup workGroup = new NioEventLoopGroup(1,new ThreadFactory() {
		private final AtomicInteger threadIndex = new AtomicInteger(0);
		@Override
		public Thread newThread(Runnable r) {
			int tempIndex = threadIndex.incrementAndGet();
			return new Thread(r,String.format("IO_THREAD_%d",tempIndex));
		}
	});
	
	public EventGroupMgr() {
		
	}
	
	public EventLoopGroup getBossGroup() {
		return this.bossGroup;
	}
	
	public EventLoopGroup getWorkGroup() {
		return this.workGroup;
	}
}
