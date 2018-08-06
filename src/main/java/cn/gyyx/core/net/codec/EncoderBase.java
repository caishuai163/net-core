package cn.gyyx.core.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public abstract class EncoderBase extends ChannelOutboundHandlerAdapter {

    /**
     * 调用继承类中{@link ChannelOutboundHandlerAdapter#write(ChannelHandlerContext, Object, ChannelPromise)}方法
     *
     * @param ctx
     * @param byteBuf
     * @param promise
     * @throws Exception
     *             void
     */
    protected void write(ChannelHandlerContext ctx, ByteBuf byteBuf,
            ChannelPromise promise) throws Exception {
        super.write(ctx, byteBuf, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
    }
}
