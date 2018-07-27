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

    public void start(String ip, int port) throws Exception {
        moduleService.startServer(ip, port);
    }

}
