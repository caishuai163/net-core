package cn.gyyx.core.net.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public class CloseUtil {

    public static ChannelFuture closeQuietly(Channel channel) {
        if (channel == null) {
            return null;
        }
        try {
            return channel.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
