package cn.gyyx.core.net.codec;

import java.nio.ByteBuffer;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.mgr.ClientSessionMgr;
import cn.gyyx.core.net.mgr.ConnectMgr;
import cn.gyyx.core.net.protocol.ProtoType;
import cn.gyyx.core.net.util.CRCUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * <h3>服务器端数据解码用</h3>实际是在客户端对服务器端的数据进行解码操作
 */
public class ServerDecoder extends DecoderBase {

    private static final int MAX_CLIENT_PACKAGE_LENGTH = 1024;

    private ClientSessionMgr clientSessionMgr;
    private ConnectMgr connectMgr;

    public ServerDecoder(ConnectMgr connectMgr) {
        super(MAX_CLIENT_PACKAGE_LENGTH, connectMgr.getProtoHandlerMgr());
        this.connectMgr = connectMgr;
        this.clientSessionMgr = connectMgr.getClientSessionMgr();
    }

    /**
     * 重写方法，用于客户端接收服务器端消息并进行处理用</br>
     * 详情的其他信息见{@link ClientDecoder#extractFrame(ChannelHandlerContext, ByteBuf, int, int)}
     */
    @Override
    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer,
            int index, int length) {
        ByteBuf byteBuf = buffer.slice(index, length);
        long requestId = byteBuf.readLong();
        int protoEnumInt = byteBuf.readInt();
        long sign = byteBuf.readLong();
        ByteBuffer byteBuffer = ByteBuffer.allocate(length - 8);

        GeneratedMessage result = null;
        ResultInfo resultInfo = new ResultInfo();
        /** 这里处理的是心跳包 */
        if (protoEnumInt == ProtoType.P_MODULE_COMMON_ACK) {
            clientSessionMgr.pingHandler(ctx.channel());
            return Unpooled.EMPTY_BUFFER;
        }
        /** 长度大于20时证明有protobuf数据,进行详细的解析数据操作 */
        if (length > 20) {
            try {
                byteBuffer.putLong(requestId);
                byteBuffer.putInt(protoEnumInt);
                result = this.readFrame(byteBuf, protoEnumInt);
                byteBuffer.put(result.toByteArray());
            } catch (Exception e) {
                e.printStackTrace();
                resultInfo.setErrorCode(StatusCode.EXCEPTION);
                /** 这里告诉客户端的消费者,你的请求发生异常了(返回结果信息，结果是异常) */
                clientSessionMgr.onServerProtoCome(requestId, ctx.channel(),
                    resultInfo);
                return Unpooled.EMPTY_BUFFER;
            }
        } else {
            byteBuffer.putLong(requestId);
            byteBuffer.putInt(protoEnumInt);
        }
        /** 获取并校验crc签名 byteBuffer实际上是用来计算签名用的 */
        long tmpSign = CRCUtil.Generic(byteBuffer.array());

        if (sign != tmpSign) {
            resultInfo.setErrorCode(StatusCode.SIGNERROR);
            /** 这里告诉客户端的消费者,你的请求返回的数据签名错了(返回结果信息，结果是SIGNERROR) */
            clientSessionMgr.onServerProtoCome(requestId, ctx.channel(),
                resultInfo);
            return Unpooled.EMPTY_BUFFER;
        }
        /** 这里是用来判断protoEnumInt的，如果小于0，必然是不正常的数据，我们不可能出现这种小于0的 */
        if (protoEnumInt < 0) {
            resultInfo.setErrorCode((byte) protoEnumInt);
            clientSessionMgr.onServerProtoCome(requestId, ctx.channel(),
                resultInfo);
            return Unpooled.EMPTY_BUFFER;
        }
        /** 这里用来放入正确的数据 */
        resultInfo.setErrorCode(StatusCode.SUCCESS);
        resultInfo.setData(result);
        clientSessionMgr.onServerProtoCome(requestId, ctx.channel(),
            resultInfo);
        return Unpooled.EMPTY_BUFFER;
    }

    /**
     * 未激活状态 断开channel
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        super.channelInactive(ctx);

        connectMgr.onDisconnect(ctx.channel());
    }

}
