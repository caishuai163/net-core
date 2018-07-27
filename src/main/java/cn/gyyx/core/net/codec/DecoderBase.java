package cn.gyyx.core.net.codec;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;

import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DefaultSocketChannelConfig;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public abstract class DecoderBase extends LengthFieldBasedFrameDecoder {

	protected final ThreadLocal<ProtoParser> THREAD_LOCAL_PARSER = new ThreadLocal<ProtoParser>() {

		@Override
		protected ProtoParser initialValue() {
			return new ProtoParser();
		}

	};
	
	protected ProtoHandlerMgr protoHandlerMgr;
	
	public DecoderBase(int maxFrameLength, ProtoHandlerMgr protoHandlerMgr) {
		super(maxFrameLength,0,4,0,4);
		this.protoHandlerMgr = protoHandlerMgr;
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
	}
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) {
		ChannelConfig config = ctx.channel().config();

		DefaultSocketChannelConfig socketConfig = (DefaultSocketChannelConfig)config;

		socketConfig.setPerformancePreferences(0,1,2);

		socketConfig.setAllocator(PooledByteBufAllocator.DEFAULT);

		ctx.fireChannelRegistered();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception {
		cause.printStackTrace();
	}
	
	protected GeneratedMessage readFrame(ByteBuf buffer, int protoEnumInt) throws Exception {
		
		ByteBufInputStream is = new ByteBufInputStream(buffer);
		
		ProtoParser parserCache = THREAD_LOCAL_PARSER.get();

		Parser<?> parser = parserCache.getParser(protoEnumInt);
		
		return (GeneratedMessage)parser.parseFrom(is);
	}

	protected final class ProtoParser {

		private final Map<Integer, Parser<?>> parserMap = new HashMap<>();

		public Parser<?> getParser(int protoEnumInt) throws Exception {

			Parser<?> parser = parserMap.get(protoEnumInt);

			if(null != parser) {
				return parser;
			}

			Class<?> clazz = protoHandlerMgr.getProtoClass(protoEnumInt);

			Field field = clazz.getField("PARSER");

			Parser<?> reflectParser = (Parser<?>)field.get(clazz);

			parserMap.put(protoEnumInt, reflectParser);

			return reflectParser;
		}
	}
}
