package cn.gyyx.core.net.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * <p>
 * 客户端无锁队列
 * </p>
 */
@SuppressWarnings("unchecked")
public class ClientNonLockQueue {

    private static Disruptor<ClientEventInfo> disruptor;

    private static RingBuffer<ClientEventInfo> ringBuffer;

    private static final int INIT_LOGIC_EVENT_CAPACITY = 1024 * 128;

    private static final WaitStrategy YIELDING_WAIT = new SleepingWaitStrategy();

    /**
     * 客户端无锁队列启动
     *
     * @param eventhandler
     *            客户端处理事件（消费者）
     * @throws Exception
     *             void
     */
    public static void start(QueueClientConsumer eventhandler)
            throws Exception {
        /** 创建一个单线程化的线程池，它只会用唯一的工作线程来执行任务，保证所有任务按照指定顺序(FIFO, LIFO, 优先级)执行。 */
        ExecutorService executor = Executors.newSingleThreadExecutor();
        /**
         * 创建客户端disrupter事件工厂
         */
        ClientEventFactory eventFactory = new ClientEventFactory();
        /**
         * 通过工厂初始化disrupter
         */
        disruptor = new Disruptor<ClientEventInfo>(eventFactory,
                INIT_LOGIC_EVENT_CAPACITY, executor, ProducerType.MULTI,
                YIELDING_WAIT);
        /**
         * 注入消费者。这里可以注入多个消费者，以数组的形式
         */
        disruptor.handleEventsWith(eventhandler);
        /**
         * 初始化全局的ringBuffer
         */
        ringBuffer = disruptor.getRingBuffer();
        /**
         * 初始化客户端消费事件
         */
        eventhandler.init();

        /**
         * 启动disrupter
         */
        disruptor.start();
    }

    /**
     * 客户端无锁队列发布客户端事件(生产者生产过程)
     *
     * @param eventInfo
     *            void
     */
    public static void publish(ClientEventInfo eventInfo) {
        /** 获取 ringBuffer中下一个可使用的序列号。 */
        long sequence = ringBuffer.next();

        try {
            /** 获取对应的事件对象 */
            ClientEventInfo newEventInfo = ringBuffer.get(sequence);

            /** 将数据写入事件对象 */
            newEventInfo.setId(eventInfo.getId());
            newEventInfo.setServiceName(eventInfo.getServiceName());
            newEventInfo.setBody(eventInfo.getBody());
            newEventInfo.setStrategryType(eventInfo.getStrategryType());
        } finally {
            /** 将事件提交到 RingBuffer */
            ringBuffer.publish(sequence);
        }
    }

    public static void stop() {
        disruptor.shutdown();
    }
}
