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

public class TcpServerMgr {

	private EventGroupMgr eventGroupMgr = null;
	
	private ServerSessionMgr severSessionMgr = null;
	
	private TimerMgr timerMgr = null;
	
	private ProtoHandlerMgr protoHandlerMgr = null;
	
	public TcpServerMgr(EventGroupMgr eventGroupMgr, ServerSessionMgr severSessionMgr, TimerMgr timerMgr, ProtoHandlerMgr protoHandlerMgr) {
		this.eventGroupMgr = eventGroupMgr;
		this.severSessionMgr = severSessionMgr;
		this.timerMgr = timerMgr;
		this.timerMgr.add(new DefaultTimer(this.severSessionMgr::closeTimeoutSession));
		this.protoHandlerMgr = protoHandlerMgr;
	}
	
	public ServerSessionMgr getServerSessionMgr() {
		return this.severSessionMgr;
	}
	
	public EndPoint acceptService(String ip, int port) throws InterruptedException {
		ServerBootstrap serverBootstrap = new ServerBootstrap();

		serverBootstrap.group(eventGroupMgr.getBossGroup(), eventGroupMgr.getWorkGroup());
		serverBootstrap.channel(NioServerSocketChannel.class);
		serverBootstrap.option(ChannelOption.SO_REUSEADDR,true);
		serverBootstrap.option(ChannelOption.SO_BACKLOG,2000);
		serverBootstrap.option(ChannelOption.SO_KEEPALIVE,true);
		serverBootstrap.option(ChannelOption.TCP_NODELAY,true);
		serverBootstrap.option(ChannelOption.SO_RCVBUF,64*1024);
		serverBootstrap.option(ChannelOption.SO_SNDBUF,64*1024);
		serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE,true);
		serverBootstrap.childOption(ChannelOption.TCP_NODELAY,true);
		serverBootstrap.childOption(ChannelOption.SO_LINGER,0);
		serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {

				ChannelPipeline pipeLine = ch.pipeline();

				pipeLine.addLast(new ClientDecoder(severSessionMgr, protoHandlerMgr));
				pipeLine.addLast(new ServerEncoder(protoHandlerMgr));
			}
		});

		serverBootstrap.bind(port).sync().channel();

		return new EndPoint(ip,port);
	}
}
