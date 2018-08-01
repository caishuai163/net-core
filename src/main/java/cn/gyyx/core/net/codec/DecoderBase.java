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

/**
 * 解码Base
 */
public abstract class DecoderBase extends LengthFieldBasedFrameDecoder {

    protected final ThreadLocal<ProtoParser> THREAD_LOCAL_PARSER = new ThreadLocal<ProtoParser>() {

        @Override
        protected ProtoParser initialValue() {
            return new ProtoParser();
        }

    };

    protected ProtoHandlerMgr protoHandlerMgr;

    public DecoderBase(int maxFrameLength, ProtoHandlerMgr protoHandlerMgr) {
        super(maxFrameLength, 0, 4, 0, 4);
        this.protoHandlerMgr = protoHandlerMgr;
    }

    /** 以下几个方法，我认为可以删掉了，实现父类的方法却又调用父类的实现 */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx)
            throws Exception {
        super.channelUnregistered(ctx);
    }

    /**
     * 重载channel的注册方法，修改channel的config配置
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        ChannelConfig config = ctx.channel().config();

        DefaultSocketChannelConfig socketConfig = (DefaultSocketChannelConfig) config;

        socketConfig.setPerformancePreferences(0, 1, 2);

        socketConfig.setAllocator(PooledByteBufAllocator.DEFAULT);

        ctx.fireChannelRegistered();
    }

    /**
     * 重载异常捕捉，处理为打印异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
    }

    /**
     * 将数据转换
     *
     * @param buffer
     * @param protoEnumInt
     * @return {@link GeneratedMessage}
     * @throws Exception
     */
    protected GeneratedMessage readFrame(ByteBuf buffer, int protoEnumInt)
            throws Exception {

        ByteBufInputStream is = new ByteBufInputStream(buffer);
        /** 从线程组中拿出一个protoBUf解析器 */
        ProtoParser parserCache = THREAD_LOCAL_PARSER.get();
        /** 找到对应数据类型的解析器 */
        Parser<?> parser = parserCache.getParser(protoEnumInt);
        /** 解析数据 */
        return (GeneratedMessage) parser.parseFrom(is);
    }

    protected final class ProtoParser {
        /** 用作缓存解析器的map */
        private final Map<Integer, Parser<?>> parserMap = new HashMap<>();

        /**
         * 获取解析器
         * 
         * @param protoEnumInt
         * @return {@link Parser}
         * @throws Exception
         */
        public Parser<?> getParser(int protoEnumInt) throws Exception {
            /** 缓存中获取解析器 */
            Parser<?> parser = parserMap.get(protoEnumInt);

            if (null != parser) {
                return parser;
            }
            /** 从protobuf数据解析处理管理器中获取对应id的class,并转换为parser */
            Class<?> clazz = protoHandlerMgr.getProtoClass(protoEnumInt);
            Field field = clazz.getField("PARSER");

            Parser<?> reflectParser = (Parser<?>) field.get(clazz);
            /** 缓存起来 */
            parserMap.put(protoEnumInt, reflectParser);

            return reflectParser;
        }
    }
}
