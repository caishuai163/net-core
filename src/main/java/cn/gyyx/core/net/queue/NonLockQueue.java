package cn.gyyx.core.net.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
  * <p>
  *   NonLockQueue描述
  * </p>
  *  
  * @author caishuai
  * @since 0.0.1
  */
/**
 * <p>
 * NonLockQueue描述
 * </p>
 * 
 * @author caishuai
 * @since 0.0.1
 */
@SuppressWarnings("unchecked")
public class NonLockQueue {

    private static Disruptor<EventInfo> disruptor;

    private static RingBuffer<EventInfo> ringBuffer;

    private static final int INIT_LOGIC_EVENT_CAPACITY = 1024 * 128;
    /**
     * <strong>指定等待策略 Disruptor</strong> </br>
     * 定义了 com.lmax.disruptor.WaitStrategy 接口用于抽象 Consumer 如何等待新事件，这是策略模式的应用。
     * Disruptor 提供了多个 WaitStrategy 的实现，每种策略都具有不同性能和优缺点，根据实际运行环境的 CPU
     * 的硬件特点选择恰当的策略，并配合特定的 JVM 的配置参数，能够实现不同的性能提升。
     * <ul>
     * <li>BlockingWaitStrategy
     * 是最低效的策略，但其对CPU的消耗最小并且在各种不同部署环境中能提供更加一致的性能表现；</li>
     * <li>SleepingWaitStrategy 的性能表现跟 BlockingWaitStrategy 差不多，对
     * CPU的消耗也类似，但其对生产者线程的影响最小，适合用于异步日志类似的场景；</li>
     * <li>YieldingWaitStrategy 的性能是最好的，适合用于低延迟的系统。在要求极高性能且事件处理线数小于 CPU
     * 逻辑核心数的场景中，推荐使用此策略；例如，CPU开启超线程的特性。</li>
     * </ul>
     */
    private static final WaitStrategy YIELDING_WAIT = new YieldingWaitStrategy();

    /**
     * 无锁队列启动
     *
     * @param eventhandler
     *            服务端消费者
     * @param ip
     *            消费者的ip
     * @param port
     *            消费者的端口号
     * @throws Exception
     */
    public static void start(QueueServerConsumer eventhandler, String ip,
            int port) throws Exception {
        /** 创建一个单线程化的线程池，它只会用唯一的工作线程来执行任务，保证所有任务按照指定顺序(FIFO, LIFO, 优先级)执行。 */
        ExecutorService executor = Executors.newSingleThreadExecutor();
        /**
         * 创建disrupter事件工厂
         */
        DefaultEventFactory eventFactory = new DefaultEventFactory();
        /** 初始化消费者 */
        eventhandler.init();

        /** 初始化disrupter */
        disruptor = new Disruptor<EventInfo>(eventFactory,
                INIT_LOGIC_EVENT_CAPACITY, executor, ProducerType.MULTI,
                YIELDING_WAIT);
        /** 初始化disrupter消费者事件 */
        disruptor.handleEventsWith(eventhandler);
        /** 初始化ringBuffer */
        ringBuffer = disruptor.getRingBuffer();
        /** disrupter 启动 */
        disruptor.start();

        /** 消费者启动 */
        eventhandler.start(ip, port);
    }

    /**
     * <h3>disruptor 事件发布</h3>
     * 发布后，消费者会监听到，调用消费者的OnEvent事件{@link QueueServerConsumer#onEvent(EventInfo, long, boolean)}
     * 
     * @param eventInfo
     *            disruptor 事件
     */
    public static void publish(EventInfo eventInfo) {
        /** 获取 ringBuffer中下一个可使用的序列号。 */
        long sequence = ringBuffer.next();

        try {
            /** 获取对应的事件对象 */
            EventInfo newEventInfo = ringBuffer.get(sequence);
            /** 将数据写入事件对象 */
            newEventInfo.setRequestId(eventInfo.getRequestId());
            newEventInfo.setBody(eventInfo.getBody());
            newEventInfo.setEventType(eventInfo.getEventType());
            newEventInfo.setChannel(eventInfo.getChannel());
            newEventInfo.setProtoEnum(eventInfo.getProtoEnum());
        } finally {
            /** 将事件提交到 RingBuffer */
            ringBuffer.publish(sequence);
        }
    }

    /**
     * 停止disrupter
     */
    public static void stop() {
        disruptor.shutdown();
    }
}
