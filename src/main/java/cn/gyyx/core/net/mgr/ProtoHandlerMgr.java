package cn.gyyx.core.net.mgr;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.protocol.ProtoHandler;
import io.netty.channel.Channel;

public class ProtoHandlerMgr {

	private Map<Integer, ProtoHandler> protoHandlers = new HashMap<>();
	
	private Map<Integer, Class<? extends GeneratedMessage>> protoNoMappers = new HashMap<>();
	private Map<Class<? extends GeneratedMessage>, Integer> protoMsgMappers = new HashMap<>();
	
	public void registerHandler(int protoNo, Class<? extends GeneratedMessage> protoClass, ProtoHandler clientProtoHandler) {
		protoHandlers.put(protoNo, clientProtoHandler);
		protoNoMappers.put(protoNo, protoClass);
		protoMsgMappers.put(protoClass, protoNo);
	}
	
	public void registerProto(int protoNo, Class<? extends GeneratedMessage> protoClass) {
		protoNoMappers.put(protoNo, protoClass);
		protoMsgMappers.put(protoClass, protoNo);
	}
	
	public void registerHandler(Class<? extends GeneratedMessage> protoClass, ProtoHandler clientProtoHandler) {
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
	
	public Class<? extends GeneratedMessage> getProtoClass(int protoEnum) {
		return protoNoMappers.get(protoEnum);
	}
	
	public int getProtoInt(Class<? extends GeneratedMessage> protoClass) {
		return protoMsgMappers.get(protoClass);
	}
	
	public GeneratedMessage handleClientProto(long requestId, int protoEnum, Channel channel, GeneratedMessage proto) {
		
		ProtoHandler handler = protoHandlers.get(protoEnum);
		
		return handler.handle(requestId, channel,  proto);
	}
	
}
