package cn.gyyx.core.net.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

@SuppressWarnings("unchecked")
public class ClientNonLockQueue {

	private static Disruptor<ClientEventInfo> disruptor;
	
	private static RingBuffer<ClientEventInfo> ringBuffer;
	
	private static final int INIT_LOGIC_EVENT_CAPACITY = 1024 * 128;
	
	private static final WaitStrategy YIELDING_WAIT = new SleepingWaitStrategy();
	
	
	public static void start(QueueClientConsumer eventhandler) throws Exception {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		
		ClientEventFactory eventFactory = new ClientEventFactory();

		disruptor = new Disruptor<ClientEventInfo>(eventFactory, INIT_LOGIC_EVENT_CAPACITY, executor, ProducerType.MULTI, YIELDING_WAIT);
		disruptor.handleEventsWith(eventhandler);
		
		ringBuffer = disruptor.getRingBuffer();
		
		eventhandler.init();
		
		disruptor.start();
	}

	public static void publish(ClientEventInfo eventInfo) {
		
		long sequence = ringBuffer.next();
		
		try {
			ClientEventInfo newEventInfo = ringBuffer.get(sequence);
			
			newEventInfo.setId(eventInfo.getId());
			newEventInfo.setServiceName(eventInfo.getServiceName());
			newEventInfo.setBody(eventInfo.getBody());
			newEventInfo.setStrategryType(eventInfo.getStrategryType());
		} finally {
			ringBuffer.publish(sequence);
		}
	}
	
	public static void stop() {
		disruptor.shutdown();
	}
}
