package cn.gyyx.core.net.codec;

import com.google.protobuf.GeneratedMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public abstract class EncoderBase extends ChannelOutboundHandlerAdapter {
	
	
	protected final ThreadLocal<EncoderMrg> localEncoderMrg = new ThreadLocal<EncoderMrg>() {
		
		protected EncoderMrg initialValue(){
			return new EncoderMrg();
		}
	};

	protected final class EncoderMrg {

		protected int get(GeneratedMessage protoObj) {
			
			return 0;
			
		}
	}

	protected void write(ChannelHandlerContext ctx, ByteBuf byteBuf, ChannelPromise promise) throws Exception {
		super.write(ctx,byteBuf,promise);
	}
	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.channel().close();
	}
}
