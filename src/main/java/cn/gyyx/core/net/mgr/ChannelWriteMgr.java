package cn.gyyx.core.net.mgr;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public class ChannelWriteMgr {

    public ChannelWriteMgr() {
    	
    }

    public ChannelFuture write(Channel channel, Object proto) {
        return channel.write(proto);
    }

    public ChannelFuture writeAndFlush(Channel channel, Object proto) {
        return channel.writeAndFlush(proto);
    }
}
