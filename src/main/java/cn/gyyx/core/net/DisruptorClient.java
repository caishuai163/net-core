package cn.gyyx.core.net;

import cn.gyyx.core.net.queue.EventInfo;
import cn.gyyx.core.net.queue.NonLockQueue;

/**
 * <p>
 * Disruptor Client
 * </p>
 */
public class DisruptorClient {

    public static void main(String[] args) {

        System.out.println("开始启动");
        // 注册及发布n=1个disruptor事件
        for (int i = 0; i < 1; i++) {
            EventInfo eventInfo = new EventInfo();
            /** 无锁队列 disrupter 发布事件 */
            NonLockQueue.publish(eventInfo);
        }

        System.out.println("启动成功");
    }
}
