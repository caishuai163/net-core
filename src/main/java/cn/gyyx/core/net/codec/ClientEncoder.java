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
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
	
		if(msg instanceof GeneratedMessage) {
			GeneratedMessage generatedMsg = (GeneratedMessage)msg;
			writeImpl(ctx,promise,generatedMsg);
		} else {
			throw new Exception("not support parameter");
		}
	}
	
	protected void writeImpl(ChannelHandlerContext ctx,ChannelPromise promise, GeneratedMessage generatedMsg) throws Exception {
		
		int protoEnumInt = protoHandlerMgr.getProtoInt(generatedMsg.getClass());
		int protoLength = generatedMsg.getSerializedSize();

		int finalProtoLen = 0;
		ByteBuf byteBuf = null;
		
		finalProtoLen = protoLength + 20;
		byteBuf = ctx.alloc().directBuffer(finalProtoLen + 4, finalProtoLen + 4);
		byteBuf.writeInt(finalProtoLen);
		long requestId = sessionMgr.getRequestId(ctx.channel());
		byteBuf.writeLong(requestId);
		byteBuf.writeInt(protoEnumInt);

		ByteBuffer byteBuffer = ByteBuffer.allocate(finalProtoLen-8);
		byteBuffer.putLong(requestId);
		byteBuffer.putInt(protoEnumInt);
		byteBuffer.put(generatedMsg.toByteArray());
		long sign = CRCUtil.Generic(byteBuffer.array());
		byteBuf.writeLong(sign);

		try(ByteBufOutputStream out = new ByteBufOutputStream(byteBuf)) {

			generatedMsg.writeTo(out);

			super.write(ctx,out.buffer(),promise);
		}
	}

}
