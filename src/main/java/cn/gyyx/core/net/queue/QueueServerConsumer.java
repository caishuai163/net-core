package cn.gyyx.core.net.queue;

import java.util.EnumMap;
import java.util.Map;

import com.lmax.disruptor.EventHandler;
import cn.gyyx.core.net.module.ModuleServerService;

/**
 * 服务端队列消费者
 */
public class QueueServerConsumer implements EventHandler<EventInfo> {

    protected ModuleServerService moduleService;

    protected final Map<EventType, AsynchronizedEventHandler> eventHandlers = new EnumMap<>(
            EventType.class);

    public QueueServerConsumer(ModuleServerService moduleService) {

        this.moduleService = moduleService;
    }

    /**
     * <h3>服务端消费者初始化</h3></br>
     * 缓存事件handler处理类型和处理方法的映射</br>
     * 服务端业务处理service.init方法
     *
     * @throws Exception
     */
    protected void init() throws Exception {

        eventHandlers.put(EventType.CLIENT_REGISTER,
            moduleService::onClientRegister);
        eventHandlers.put(EventType.CLIENT_PROTO_COMMING,
            moduleService::onClientProtoCome);
        eventHandlers.put(EventType.CLIENT_DISCONNECT,
            moduleService::onClientDisconnect);

        moduleService.init();
    }

    @Override
    public void onEvent(EventInfo event, long sequence, boolean endOfBatch) {

        EventType eventType = event.getEventType();

        AsynchronizedEventHandler handler = eventHandlers.get(eventType);

        handler.onEvent(event);
    }

    /**
     * 消费者启动事件
     *
     * @param ip
     * @param port
     * @throws Exception
     */
    public void start(String ip, int port) throws Exception {
        moduleService.startServer(ip, port);
    }

}
