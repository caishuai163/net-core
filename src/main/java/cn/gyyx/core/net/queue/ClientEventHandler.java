package cn.gyyx.core.net.queue;

/**
 * <p>
 * 客户端处理事件（消费者）
 * </p>
 * 
 * @author caishuai
 * @since 0.0.1
 */
public interface ClientEventHandler {
    /**
     * 消费操作
     * 
     * @param parameter
     * @return Object
     */
    Object handle(Object parameter);
}
