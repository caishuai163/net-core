package cn.gyyx.core.net.mgr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.gyyx.core.net.codec.ResultInfo;
import cn.gyyx.core.net.module.SyncContext;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ping;
import cn.gyyx.core.net.service.ServiceEntry;
import cn.gyyx.core.net.service.ServiceRegister;
import cn.gyyx.core.net.util.CloseUtil;
import cn.gyyx.core.net.util.SystemTimeUtil;
import io.netty.channel.Channel;

public class ClientSessionMgr  {

	private Map<String, Map<String, Channel>> serviceChannels = new ConcurrentHashMap<>(); 
	
	private ChannelWriteMgr channelMgr;
	private ServiceRegister serviceRegister;
	private Map<Channel, ClientSessionInfo> sessions = new ConcurrentHashMap<>();
	private int heatSecond = 600000;
	
	public ClientSessionMgr(ChannelWriteMgr channelMgr, ServiceRegister serviceRegister) {
		this.channelMgr = channelMgr;
		this.serviceRegister = serviceRegister;
	}
	
	public void addSession(String serviceName, String address, ClientSessionInfo session) {
		
		sessions.put(session.getChannel(), session);
		
		Map<String, Channel> channels = serviceChannels.get(serviceName);
		
		if(channels != null) {
			
			channels.putIfAbsent(address, session.getChannel());
		} else {
			Map<String, Channel> newChannels = new ConcurrentHashMap<>();
			
			newChannels.put(address, session.getChannel());
			
			serviceChannels.putIfAbsent(serviceName, newChannels);
		}
	}
	
	public ClientSessionInfo getSession(Channel channel) {
		return sessions.get(channel);
	}
	
	
	public long getRequestId(Channel channel) {
		
		ClientSessionInfo session = sessions.get(channel);
		
		SyncContext context = session.getSyncContext();
		
		return context.getSyncId();
	}
	
	public Channel getChannel(String serviceName, String address) {
		
		Map<String, Channel> channels = serviceChannels.get(serviceName);
		
		if(channels == null) {
			return null;
		}
		
		return channels.get(address);
	}
	
	public void removeSession(Channel channel) {

		try {
			ClientSessionInfo session = getSession(channel);
			
			if(session != null) {
				sessions.remove(channel);

				Map<String, Channel> channels = serviceChannels.get(session.getServiceName());
				
				if(channels != null) {
					
					String address = session.getRemoteIp() + ":" + session.getRemotePort();

					Channel tmpChannel = channels.get(address);
					
					if(tmpChannel != null && tmpChannel == channel) {
						channels.remove(address);
					}
				}
			}
			
		} catch(Throwable ex) {
			ex.printStackTrace();
		}
	}
	
	public void updateSyncContext(SyncContext syncContext, Channel channel) {
		
		ClientSessionInfo session = sessions.get(channel);
		
		if(session != null) {
			session.setSyncContext(syncContext);
		}
	}
	
	public void sendPing() {
		for(Map.Entry<Channel, ClientSessionInfo> entry : sessions.entrySet()) {
			
			p_module_common_ping.Builder builder = p_module_common_ping.newBuilder();
			
			sendMsg(entry.getKey(), builder.build());
		}
	}
	
	public void checkTimeoutSession() {
		for(Map.Entry<Channel, ClientSessionInfo> entry : sessions.entrySet()) {
			
			ClientSessionInfo session = entry.getValue();

			int curTime = SystemTimeUtil.getTimestamp();
			int lastTime = session.getLastPingSec();

			if(curTime - lastTime > heatSecond) {

				Map<String, Channel> channels = serviceChannels.get(session.getServiceName());
				
				if(channels != null) {
					
					ServiceEntry serviceEntry = new ServiceEntry();
					
					serviceEntry.setServiceName(session.getServiceName());
					serviceEntry.setIp(session.getRemoteIp());
					serviceEntry.setPort(session.getRemotePort());

					try {
						serviceRegister.unregisterService(serviceEntry);
					} catch (Exception e) {					
						e.printStackTrace();
					}
				}
				
				System.out.println("客户端会话关闭");
				removeSession(entry.getKey());
				CloseUtil.closeQuietly(session.getChannel());
			}
		}
	}
	
	public void onServerProtoCome(long requestId, Channel channel, ResultInfo result) {
		ClientSessionInfo sessionInfo = getSession(channel);
		
		if(sessionInfo == null) {
			return;
		}
		
		SyncContext context = sessionInfo.getSyncContext();

		if(context == null) {
			return;
		}
		
		if(context.getSyncId() != requestId) {
			return;
		}
				
		context.setResult(result);
	}
	
	public void sendMsg(Channel channel, Object proto) {
		
		channelMgr.writeAndFlush(channel, proto);
	}
	
	public void pingHandler(Channel channel) {
		ClientSessionInfo session = sessions.get(channel);
		
		if(session != null) {
			session.setLastPingSec(SystemTimeUtil.getTimestamp());
		}
	}
	
}
