package cn.gyyx.core.net.mgr;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * <p>Channel数据写入管理</p>
 */
public class ChannelWriteMgr {

    public ChannelWriteMgr() {

    }

    /**
     * 向channel中写入protobuf数据，not immediately
     *
     * @param channel
     * @param proto
     * @return ChannelFuture
     */
    public ChannelFuture write(Channel channel, Object proto) {
        return channel.write(proto);
    }

    /**
     * 向channel中写入protobuf数据,immediately flush
     * 
     * @param channel
     * @param proto
     * @return ChannelFuture
     */
    public ChannelFuture writeAndFlush(Channel channel, Object proto) {
        return channel.writeAndFlush(proto);
    }
}
