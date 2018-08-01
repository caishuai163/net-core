package cn.gyyx.core.net.mgr;

import cn.gyyx.core.net.codec.ClientDecoder;
import cn.gyyx.core.net.codec.ServerEncoder;
import cn.gyyx.core.net.service.EndPoint;
import cn.gyyx.core.net.timer.DefaultTimer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * tcp服务管理类
 */
public class TcpServerMgr {

    private EventGroupMgr eventGroupMgr = null;

    private ServerSessionMgr severSessionMgr = null;

    private TimerMgr timerMgr = null;

    private ProtoHandlerMgr protoHandlerMgr = null;

    /**
     * 创建tcp服务管理实例
     * 
     * @param eventGroupMgr
     *            {@link EventGroupMgr}
     * @param severSessionMgr
     *            {@link ServerSessionMgr}
     * @param timerMgr
     *            {@link TimerMgr}
     * @param protoHandlerMgr
     *            {@link ProtoHandlerMgr}
     */
    public TcpServerMgr(EventGroupMgr eventGroupMgr,
            ServerSessionMgr severSessionMgr, TimerMgr timerMgr,
            ProtoHandlerMgr protoHandlerMgr) {
        this.eventGroupMgr = eventGroupMgr;
        this.severSessionMgr = severSessionMgr;
        this.timerMgr = timerMgr;
        /** 添加定时任务 --关闭超时的session */
        this.timerMgr.add(
            new DefaultTimer(this.severSessionMgr::closeTimeoutSession));
        this.protoHandlerMgr = protoHandlerMgr;
    }

    public ServerSessionMgr getServerSessionMgr() {
        return this.severSessionMgr;
    }

    /**
     * 创建监听
     *
     * @param ip
     * @param port
     * @return {@link EndPoint}
     * @throws InterruptedException
     */
    public EndPoint acceptService(String ip, int port)
            throws InterruptedException {
        /** 快速创建netty */
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        /** 各种各种配置 TODO 详细每一个是作甚用途的 */
        serverBootstrap.group(eventGroupMgr.getBossGroup(),
            eventGroupMgr.getWorkGroup());
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 2000);
        serverBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        serverBootstrap.option(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.option(ChannelOption.SO_RCVBUF, 64 * 1024);
        serverBootstrap.option(ChannelOption.SO_SNDBUF, 64 * 1024);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.childOption(ChannelOption.SO_LINGER, 0);
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            /**
             * 初始化channel的执行事件
             */
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {

                ChannelPipeline pipeLine = ch.pipeline();
                /** 客户端解码，服务器端编码 */
                pipeLine.addLast(
                    new ClientDecoder(severSessionMgr, protoHandlerMgr));
                pipeLine.addLast(new ServerEncoder(protoHandlerMgr));
            }
        });
        /** 创建netty channel监听端口 */
        serverBootstrap.bind(port).sync().channel();

        return new EndPoint(ip, port);
    }
}
