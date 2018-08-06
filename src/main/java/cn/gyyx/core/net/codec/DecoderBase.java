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
 * 解码Base</br>
 */
public abstract class DecoderBase extends LengthFieldBasedFrameDecoder {

    protected final ThreadLocal<ProtoParser> THREAD_LOCAL_PARSER = new ThreadLocal<ProtoParser>() {

        @Override
        protected ProtoParser initialValue() {
            return new ProtoParser();
        }

    };

    protected ProtoHandlerMgr protoHandlerMgr;

    /**
     * <li>1. maxFrameLength - 发送的数据帧最大长度</li>
     * <li>2. lengthFieldOffset -
     * 定义长度域位于发送的字节数组中的下标。换句话说：发送的字节数组中下标为${lengthFieldOffset}的地方是长度域的开始地方</li>
     * <li>3. lengthFieldLength - 用于描述定义的长度域的长度。换句话说：发送字节数组bytes时,
     * 字节数组bytes[lengthFieldOffset,
     * lengthFieldOffset+lengthFieldLength]域对应于的定义长度域部分</li>
     * <li>4. lengthAdjustment - 协议体长度调节值，修正信息长度，如果设置为4，那么解码时再向后推4个字节</li>
     * <li>5. initialBytesToStrip - 跳过字节数，如我们想跳过长度属性部分</li>
     * <li>6. failFast - true: 读取到长度域超过maxFrameLength，就抛出一个
     * TooLongFrameException。false: 只有真正读取完长度域的值表示的字节之后，才会抛出
     * TooLongFrameException，默认情况下设置为true，建议不要修改，否则可能会造成内存溢出</li>
     * <li>7. ByteOrder - 数据存储采用大端模式或小端模式</li>
     * 
     * @param maxFrameLength
     * @param protoHandlerMgr
     */
    public DecoderBase(int maxFrameLength, ProtoHandlerMgr protoHandlerMgr) {
        /**
         * 这里的第一个0和4是从0开始读，读4位得到数据的长度</br>
         * 第二个0是数据偏移量，自己写的数据没有偏移量 第二个4代表开始读数据是从第几位开始读，即去除代表长度的四位开始读数据
         */
        super(maxFrameLength, 0, 4, 0, 4);
        this.protoHandlerMgr = protoHandlerMgr;
    }

    /** 以下几个方法，删掉了，实现父类的方法却又调用父类的实现 */

    /**
     * 重载channel的注册方法，修改channel的config配置
     * <li>设置连接时间、潜伏期、带宽</li>
     * <li>用于信道的BytBuffLoalPosit分配缓冲区</li>
     * <li>channel注册</li>
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
