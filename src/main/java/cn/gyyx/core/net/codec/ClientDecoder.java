package cn.gyyx.core.net.codec;

import java.nio.ByteBuffer;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.mgr.ServerSessionMgr;
import cn.gyyx.core.net.protocol.ProtoType;
import cn.gyyx.core.net.queue.EventInfo;
import cn.gyyx.core.net.queue.EventType;
import cn.gyyx.core.net.queue.NonLockQueue;
import cn.gyyx.core.net.util.CRCUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 创建tcp的绑定后，当客户端有请求发过来的时候，会先走客户端数据解码操作
 */
public class ClientDecoder extends DecoderBase {

    private static final int MAX_SERVER_PACKAGE_LENGTH = 1024 * 128;

    private ServerSessionMgr sessionMgr;

    public ClientDecoder(ServerSessionMgr sessionMgr,
            ProtoHandlerMgr protoHandlerMgr) {
        super(MAX_SERVER_PACKAGE_LENGTH, protoHandlerMgr);
        this.sessionMgr = sessionMgr;
    }

    /**
     * 这个没有直接继承{@link ByteToMessageDecoder#decode},</br>
     * 而是实际继承的是{@link LengthFieldBasedFrameDecoder#extractFrame}.</br>
     * 实际上{@link LengthFieldBasedFrameDecoder#decode}继承了<code>ByteToMessageDecoder.decode</code>,</br>
     * 并在实现里进行了一定的处理并调用了<code>LengthFieldBasedFrameDecoder.extractFrame</code>
     */
    @Override
    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer,
            int index, int length) {
        ByteBuf byteBuf = buffer.slice(index, length);
        long requestId = byteBuf.readLong();
        int protoEnumInt = byteBuf.readInt();
        long sign = byteBuf.readLong();
        /** 这里处理的是心跳包 */
        if (protoEnumInt == ProtoType.P_MODULE_COMMON_PING) {
            sessionMgr.pingHandler(ctx.channel(), requestId);
            return Unpooled.EMPTY_BUFFER;
        }

        try {
            /** 调用baseDecode中的方法，将数据转换(protobuf) */
            GeneratedMessage protoObj = this.readFrame(byteBuf, protoEnumInt);

            /** 对数据做一次crc检验 */
            ByteBuffer byteBuffer = ByteBuffer.allocate(length - 8);
            byteBuffer.putLong(requestId);
            byteBuffer.putInt(protoEnumInt);
            byteBuffer.put(protoObj.toByteArray());
            long tmpSign = CRCUtil.Generic(byteBuffer.array());

            if (sign != tmpSign) {
                /** 校验不通过，则发送数据签名错误返回 */
                sessionMgr.sendMsg(requestId, StatusCode.SIGNERROR,
                    ctx.channel(), null);
                return Unpooled.EMPTY_BUFFER;
            }
            /** 返回服务器端回传的事件并塞入队列 */
            EventInfo eventInfo = new EventInfo();
            eventInfo.setEventType(EventType.CLIENT_PROTO_COMMING);
            eventInfo.setBody(protoObj);
            eventInfo.setRequestId(requestId);
            eventInfo.setChannel(ctx.channel());
            eventInfo.setProtoEnum(protoEnumInt);

            NonLockQueue.publish(eventInfo);
        } catch (Throwable ex) {
            ex.printStackTrace();
            sessionMgr.sendMsg(requestId, StatusCode.EXCEPTION, ctx.channel(),
                null);
        }

        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        System.out.println("服务器连接关闭");
        EventInfo eventInfo = new EventInfo();

        eventInfo.setEventType(EventType.CLIENT_DISCONNECT);
        eventInfo.setChannel(ctx.channel());

        NonLockQueue.publish(eventInfo);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        super.channelRegistered(ctx);

        EventInfo eventInfo = new EventInfo();

        eventInfo.setEventType(EventType.CLIENT_REGISTER);
        eventInfo.setChannel(ctx.channel());

        NonLockQueue.publish(eventInfo);
    }

}
