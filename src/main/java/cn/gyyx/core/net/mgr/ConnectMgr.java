package cn.gyyx.core.net.mgr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.gyyx.core.net.codec.ClientEncoder;
import cn.gyyx.core.net.codec.ServerDecoder;
import cn.gyyx.core.net.timer.DefaultTimer;
import cn.gyyx.core.net.util.SystemTimeUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ConnectMgr {

	private EventGroupMgr eventGroupMgr;
	
	private ClientSessionMgr clientSessionMgr;
	
	private TimerMgr timerMgr;
	
	private ProtoHandlerMgr protoHandlerMgr;
	
	private int tryReconnectTimeout = 600000;
	
	private List<ReconnectInfo> reconnects = new ArrayList<>();
	
	public ConnectMgr(EventGroupMgr eventGroupMgr, ClientSessionMgr clientSessionMgr, 
			TimerMgr timerMgr, ProtoHandlerMgr protoHandlerMgr) {
		this.eventGroupMgr = eventGroupMgr;
		this.clientSessionMgr  = clientSessionMgr;
		this.timerMgr = timerMgr;
		this.timerMgr.add(new DefaultTimer(clientSessionMgr::sendPing));
		this.timerMgr.add(new DefaultTimer(clientSessionMgr::checkTimeoutSession));
		this.timerMgr.add(new DefaultTimer(this::reConnect, 5000));
		this.protoHandlerMgr = protoHandlerMgr;
	}
	
	public ClientSessionMgr getClientSessionMgr() {
		return this.clientSessionMgr;
	}
	
	public ProtoHandlerMgr getProtoHandlerMgr() {
		return this.protoHandlerMgr;
	}
	
	public Channel connect(String ip, int port) {

		return doConnect(ip, port);
	}
	
	public Channel connect(String serviceName, String ip, int port) {

		try {
			String address = ip + ":" + port;
			Channel channel = clientSessionMgr.getChannel(serviceName, address);
			
			if(channel != null) {
				return channel;
			}
			
			channel = doConnect(ip, port);
			
			if(channel != null) {
				ClientSessionInfo session = new ClientSessionInfo();
				session.setServiceName(serviceName);
				session.setChannel(channel);
				session.setRemoteIp(ip);
				session.setRemotePort(port);
				session.setLastPingSec(SystemTimeUtil.getTimestamp());
				clientSessionMgr.addSession(serviceName, address, session);
			}
			
			return channel;
			
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
		
	}
	
	public void onDisconnect(Channel channel) {
		
		if(clientSessionMgr.getSession(channel) != null) {
			ReconnectInfo reconnectInfo = new ReconnectInfo();
			reconnectInfo.setChannel(channel);
			reconnectInfo.setStartTime(SystemTimeUtil.getTimestamp());
			reconnects.add(reconnectInfo);
		}
	}
	
	private void reConnect() {
		
		for(Iterator<ReconnectInfo> itr=reconnects.iterator(); itr.hasNext();) {
			
			ReconnectInfo info = itr.next();
			long endTime = SystemTimeUtil.getTimestamp();
			if(endTime - info.getStartTime() > tryReconnectTimeout) {
				itr.remove();
    			return;
    		}
    		
    		ClientSessionInfo session = clientSessionMgr.getSession(info.getChannel());

    		if(session ==  null) {
    			itr.remove();
    			return;
    		}
    		
    		Channel newchannel = doConnect(session.getRemoteIp(), session.getRemotePort());
    		
    		if(newchannel != null) {
    			itr.remove();
    			clientSessionMgr.removeSession(info.getChannel());
    			
    			String address = session.getRemoteIp() + ":" + session.getRemotePort();
    			
    			ClientSessionInfo newSession = new ClientSessionInfo();
    			newSession.setServiceName(session.getServiceName());
    			newSession.setChannel(newchannel);
    			newSession.setRemoteIp(session.getRemoteIp());
    			newSession.setRemotePort(session.getRemotePort());
    			newSession.setLastPingSec(SystemTimeUtil.getTimestamp());
    			clientSessionMgr.addSession(session.getServiceName(), address, newSession);
    		}
		}
	}
	
	private Channel doConnect(String ip, int port) {
		
		Bootstrap bootstrap = new Bootstrap();

		bootstrap.channel(NioSocketChannel.class);
		bootstrap.group(eventGroupMgr.getWorkGroup());

		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new ServerDecoder(ConnectMgr.this));
				ch.pipeline().addLast(new ClientEncoder(ConnectMgr.this));
			}
		});

		bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.SO_RCVBUF, 64*1024)
				.option(ChannelOption.SO_SNDBUF, 64*1024)
				.option(ChannelOption.SO_LINGER, 0);

		try {
			return bootstrap.connect(ip, port).sync().channel();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
