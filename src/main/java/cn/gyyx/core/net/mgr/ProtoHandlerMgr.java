package cn.gyyx.core.net.mgr;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.protocol.ProtoHandler;
import io.netty.channel.Channel;

/**
 * <p>
 * ProtoBuf处理管理类
 * </p>
 */
public class ProtoHandlerMgr {
    /** 缓存标识和handler映射关系 */
    private Map<Integer, ProtoHandler> protoHandlers = new HashMap<>();

    /** 缓存标识和数据解析器对应的类的映射关系 */
    private Map<Integer, Class<? extends GeneratedMessage>> protoNoMappers = new HashMap<>();
    /** 缓存标识和数据解析器对应的类的映射关系 反响查询用 */
    private Map<Class<? extends GeneratedMessage>, Integer> protoMsgMappers = new HashMap<>();

    public void registerHandler(int protoNo,
            Class<? extends GeneratedMessage> protoClass,
            ProtoHandler clientProtoHandler) {
        protoHandlers.put(protoNo, clientProtoHandler);
        protoNoMappers.put(protoNo, protoClass);
        protoMsgMappers.put(protoClass, protoNo);
    }

    public void registerProto(int protoNo,
            Class<? extends GeneratedMessage> protoClass) {
        protoNoMappers.put(protoNo, protoClass);
        protoMsgMappers.put(protoClass, protoNo);
    }

    public void registerHandler(Class<? extends GeneratedMessage> protoClass,
            ProtoHandler clientProtoHandler) {
        int hashCode = protoClass.getName().hashCode();
        hashCode = hashCode & 0x7FFFFFFF;
        protoHandlers.put(hashCode, clientProtoHandler);
        protoNoMappers.put(hashCode, protoClass);
        protoMsgMappers.put(protoClass, hashCode);
    }

    public void registerProto(Class<? extends GeneratedMessage> protoClass) {
        int hashCode = protoClass.getName().hashCode();
        hashCode = hashCode & 0x7FFFFFFF;
        protoNoMappers.put(hashCode, protoClass);
        protoMsgMappers.put(protoClass, hashCode);
    }

    /**
     * 获取protoEnumInt对应的protobuf数据类型
     *
     * @param protoEnum
     * @return Class<? extends GeneratedMessage>
     */
    public Class<? extends GeneratedMessage> getProtoClass(int protoEnum) {
        return protoNoMappers.get(protoEnum);
    }

    /**
     * 获取protobuf数据类型的类对应的标识位
     *
     * @param protoClass
     * @return int protoEnumInt
     */
    public int getProtoInt(Class<? extends GeneratedMessage> protoClass) {
        return protoMsgMappers.get(protoClass);
    }

    public GeneratedMessage handleClientProto(long requestId, int protoEnum,
            Channel channel, GeneratedMessage proto) {

        ProtoHandler handler = protoHandlers.get(protoEnum);

        return handler.handle(requestId, channel, proto);
    }

}
