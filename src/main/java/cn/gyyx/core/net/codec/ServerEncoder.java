package cn.gyyx.core.net.codec;

import java.nio.ByteBuffer;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.util.CRCUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class ServerEncoder extends EncoderBase {

    private ProtoHandlerMgr protoHandlerMgr;

    public ServerEncoder(ProtoHandlerMgr protoHandlerMgr) {
        this.protoHandlerMgr = protoHandlerMgr;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg,
            ChannelPromise promise) throws Exception {

        if (msg instanceof ResponseContext) {
            ResponseContext context = (ResponseContext) msg;

            writeImpl(ctx, promise, context.getRequestId(), context.getStatus(),
                context.getResult());
        } else {
            throw new Exception("not support parameter");
        }
    }

    /**
     * @see ClientEncoder#writeImpl(ChannelHandlerContext, ChannelPromise,
     *      GeneratedMessage)
     * @param ctx
     * @param promise
     * @param requestId
     * @param status
     * @param generatedMsg
     * @throws Exception
     */
    protected void writeImpl(ChannelHandlerContext ctx, ChannelPromise promise,
            long requestId, byte status, GeneratedMessage generatedMsg)
            throws Exception {

        int protoLength = 0;
        int protoInt = 0;

        if (generatedMsg == null) {
            protoInt = status;
            int finalProtoLen = protoLength + 20;
            ByteBuf byteBuf = this.getByteBuf(finalProtoLen, ctx, requestId,
                protoInt);
            ByteBuffer byteBuffer = this.getByteBuffer(finalProtoLen, requestId,
                protoInt);
            long sign = CRCUtil.Generic(byteBuffer.array());
            byteBuf.writeLong(sign);

            super.write(ctx, byteBuf, promise);
        } else {
            protoInt = protoHandlerMgr.getProtoInt(generatedMsg.getClass());
            protoLength = generatedMsg.getSerializedSize();
            int finalProtoLen = protoLength + 20;

            ByteBuf byteBuf = this.getByteBuf(finalProtoLen, ctx, requestId,
                protoInt);
            ByteBuffer byteBuffer = this.getByteBuffer(finalProtoLen, requestId,
                protoInt);

            byteBuffer.put(generatedMsg.toByteArray());
            long sign = CRCUtil.Generic(byteBuffer.array());

            byteBuf.writeLong(sign);

            try (ByteBufOutputStream out = new ByteBufOutputStream(byteBuf)) {
                generatedMsg.writeTo(out);
                super.write(ctx, out.buffer(), promise);
            }
        }

    }

    private ByteBuf getByteBuf(int finalProtoLen, ChannelHandlerContext ctx,
            long requestId, int protoInt) {

        ByteBuf byteBuf = ctx.alloc().directBuffer(finalProtoLen + 4,
            finalProtoLen + 4);
        byteBuf.writeInt(finalProtoLen);
        byteBuf.writeLong(requestId);
        byteBuf.writeInt(protoInt);

        return byteBuf;

    }

    private ByteBuffer getByteBuffer(int finalProtoLen, long requestId,
            int protoInt) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(finalProtoLen - 8);

        byteBuffer.putLong(requestId);
        byteBuffer.putInt(protoInt);

        return byteBuffer;
    }
}
