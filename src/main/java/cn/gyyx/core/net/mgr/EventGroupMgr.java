package cn.gyyx.core.net.mgr;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * 
 * <p>
 * netty事件处理reactor线程池？？？？
 * </p>
 * 服务端启动的时候，创建了两个NioEventLoopGroup，它们实际是两个独立的Reactor线程池。</br>
 * 一个用于接收客户端的TCP连接，另一个用于处理I/O相关的读写操作，或者执行系统Task、定时任务Task等。
 */
public class EventGroupMgr {
    /** 主线程组(大小为1,第二个参数为线程创建工厂) */
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1,
            (Runnable r) -> new Thread(r, "ACCEPT_THREAD"));
    /** 工作线程组(大小为1,第二个参数为线程创建工厂) */
    private EventLoopGroup workGroup = new NioEventLoopGroup(1,
            new ThreadFactory() {
                /** 定义线程安全的Integer */
                private final AtomicInteger threadIndex = new AtomicInteger(0);

                /**
                 * 实现线程工厂的newThread方法
                 */
                @Override
                public Thread newThread(Runnable r) {
                    /** 获得递增后的ID */
                    int tempIndex = threadIndex.incrementAndGet();
                    /** 返回创建的新线程 */
                    return new Thread(r,
                            String.format("IO_THREAD_%d", tempIndex));
                }
            });

    /** 空构造 */
    public EventGroupMgr() {

    }

    /**
     * 获取主线程组
     * 
     * @return EventLoopGroup
     */
    public EventLoopGroup getBossGroup() {
        return this.bossGroup;
    }

    /**
     * 获取工作线程组
     *
     * @return EventLoopGroup
     */
    public EventLoopGroup getWorkGroup() {
        return this.workGroup;
    }
}
