package cn.gyyx.core.net.queue;

import com.lmax.disruptor.EventFactory;

/**
 * <p>
 * 客户端disrupter事件工厂
 * </p>
 * 
 * @see DefaultEventFactory
 */
public class ClientEventFactory implements EventFactory<ClientEventInfo> {
    /**
     * 获取实例化对象
     */
    @Override
    public ClientEventInfo newInstance() {
        return new ClientEventInfo();
    }

}
