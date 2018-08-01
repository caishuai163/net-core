package cn.gyyx.core.net.codec;

import java.nio.ByteBuffer;
import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.mgr.ClientSessionMgr;
import cn.gyyx.core.net.mgr.ConnectMgr;
import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.util.CRCUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class ClientEncoder extends EncoderBase {

    private ClientSessionMgr sessionMgr;
    private ProtoHandlerMgr protoHandlerMgr;

    public ClientEncoder(ConnectMgr connectMgr) {
        this.sessionMgr = connectMgr.getClientSessionMgr();
        this.protoHandlerMgr = connectMgr.getProtoHandlerMgr();
    }

    /**
     * 重写方法,这里实际上是对channel写入的重写
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg,
            ChannelPromise promise) throws Exception {
        /** 判断数据类型 */
        if (msg instanceof GeneratedMessage) {
            GeneratedMessage generatedMsg = (GeneratedMessage) msg;
            /** 进行编码写入传输 */
            writeImpl(ctx, promise, generatedMsg);
        } else {
            throw new Exception("not support parameter");
        }
    }

    /**
     * 具体的进行编码写入传输操作
     * 
     * @param ctx
     * @param promise
     * @param generatedMsg
     * @throws Exception
     */
    protected void writeImpl(ChannelHandlerContext ctx, ChannelPromise promise,
            GeneratedMessage generatedMsg) throws Exception {
        /** 获取数据标识位 */
        int protoEnumInt = protoHandlerMgr.getProtoInt(generatedMsg.getClass());
        /** 获取数据的长度 */
        int protoLength = generatedMsg.getSerializedSize();

        /**
         * 这里是计算长度和申请bytebuf的内存空间, 原来是</br>
         * <code>int finalProtoLen = 0;</br>
         * ByteBuf byteBuf = null;</code></br>
         * 这里改成直接赋值了
         */
        int finalProtoLen = protoLength + 20;
        ByteBuf byteBuf = ctx.alloc().directBuffer(finalProtoLen + 4,
            finalProtoLen + 4);
        byteBuf.writeInt(finalProtoLen);
        /** 获取本次请求的requestId */
        long requestId = sessionMgr.getRequestId(ctx.channel());
        byteBuf.writeLong(requestId);
        byteBuf.writeInt(protoEnumInt);
        /** 以下几行是计算签名用的 */
        ByteBuffer byteBuffer = ByteBuffer.allocate(finalProtoLen - 8);
        byteBuffer.putLong(requestId);
        byteBuffer.putInt(protoEnumInt);
        byteBuffer.put(generatedMsg.toByteArray());
        long sign = CRCUtil.Generic(byteBuffer.array());
        byteBuf.writeLong(sign);

        try (ByteBufOutputStream out = new ByteBufOutputStream(byteBuf)) {
            // TODO
            generatedMsg.writeTo(out);
            /** 这块实际上是调用的baseEncode中的protect的write方法 */
            super.write(ctx, out.buffer(), promise);
        }
    }

}
