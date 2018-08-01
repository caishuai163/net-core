package cn.gyyx.core.net.queue;

import com.google.protobuf.GeneratedMessage;

import io.netty.channel.Channel;

/**
 * 定义disrupter事件
 */
public class EventInfo {

    private EventType eventType;

    private int protoEnum;
    /** 信道，管道。netty通信时创建的 */
    private Channel channel;

    private long requestId;

    private GeneratedMessage body;

    /**
     * @return EventType 事件类型
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * @param eventType
     *            事件类型
     */
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    /**
     * @return long 请求的唯一标识
     */
    public long getRequestId() {
        return requestId;
    }

    /**
     * @param requestId
     *            请求的唯一标识
     */
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    /**
     * @return GeneratedMessage 事件中protobuf数据
     */
    public GeneratedMessage getBody() {
        return body;
    }

    /**
     * @param body
     *            设置事件中protobuf数据
     */
    public void setBody(GeneratedMessage body) {
        this.body = body;
    }

    public int getProtoEnum() {
        return protoEnum;
    }

    public void setProtoEnum(int protoEnum) {
        this.protoEnum = protoEnum;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
