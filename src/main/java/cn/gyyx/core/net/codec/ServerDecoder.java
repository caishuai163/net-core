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

public class ServerDecoder extends DecoderBase {

	private static final int MAX_CLIENT_PACKAGE_LENGTH = 1024;

	private ClientSessionMgr clientSessionMgr;
	private ConnectMgr connectMgr;
	
	public ServerDecoder(ConnectMgr connectMgr) {
		super(MAX_CLIENT_PACKAGE_LENGTH, connectMgr.getProtoHandlerMgr());
		this.connectMgr = connectMgr;
		this.clientSessionMgr = connectMgr.getClientSessionMgr();
	}
	
	@Override
	protected ByteBuf extractFrame(ChannelHandlerContext ctx,ByteBuf buffer, int index, int length) {
		ByteBuf byteBuf = buffer.slice(index,length);
		long requestId = byteBuf.readLong();
		int protoEnumInt = byteBuf.readInt();
		long sign = byteBuf.readLong();
		ByteBuffer byteBuffer = ByteBuffer.allocate(length-8);

		GeneratedMessage result = null;
		ResultInfo resultInfo = new ResultInfo();
		
		if(protoEnumInt == ProtoType.P_MODULE_COMMON_ACK) {
			clientSessionMgr.pingHandler(ctx.channel());
			return Unpooled.EMPTY_BUFFER;
		}
		
		if(length > 20) {
			try {
				byteBuffer.putLong(requestId);
				byteBuffer.putInt(protoEnumInt);
				result = this.readFrame(byteBuf, protoEnumInt);
				byteBuffer.put(result.toByteArray());
			} catch (Exception e) {
				e.printStackTrace();
				resultInfo.setErrorCode(StatusCode.EXCEPTION);
				clientSessionMgr.onServerProtoCome(requestId, ctx.channel(), resultInfo);
				return Unpooled.EMPTY_BUFFER;
			}
		} else {
			byteBuffer.putLong(requestId);
			byteBuffer.putInt(protoEnumInt);
		}

		long tmpSign = CRCUtil.Generic(byteBuffer.array());
		
		if(sign != tmpSign) {
			resultInfo.setErrorCode(StatusCode.SIGNERROR);
			clientSessionMgr.onServerProtoCome(requestId, ctx.channel(), resultInfo);
			return Unpooled.EMPTY_BUFFER;
		}
		
		if(protoEnumInt < 0) {
			resultInfo.setErrorCode((byte)protoEnumInt);
			clientSessionMgr.onServerProtoCome(requestId, ctx.channel(), resultInfo);
			return Unpooled.EMPTY_BUFFER;
		}

		resultInfo.setErrorCode(StatusCode.SUCCESS);
		resultInfo.setData(result);
		clientSessionMgr.onServerProtoCome(requestId, ctx.channel(), resultInfo);
		return Unpooled.EMPTY_BUFFER;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {

		super.channelInactive(ctx);
		
		connectMgr.onDisconnect(ctx.channel());
	}

}
