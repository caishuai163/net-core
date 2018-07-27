package cn.gyyx.core.net.mgr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.codec.ResponseContext;
import cn.gyyx.core.net.codec.StatusCode;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ack;
import cn.gyyx.core.net.util.CloseUtil;
import cn.gyyx.core.net.util.SystemTimeUtil;
import io.netty.channel.Channel;

public class ServerSessionMgr {

	private Map<Channel, SessionInfo> sessions = new ConcurrentHashMap<>();
	
	private int heatSecond = 600000;
	
	protected ChannelWriteMgr channelMgr;

	public ServerSessionMgr(ChannelWriteMgr channelMgr) {
		this.channelMgr = channelMgr;
	}
	
	public void addSession(SessionInfo session) {
		
		sessions.put(session.getChannel(), session);
	}
	
	public void saveLastResult(long requestId, Channel channel, GeneratedMessage result) {
		
		SessionInfo session = sessions.get(channel);
		
		if(session != null) {
			session.setRequestId(requestId);
			session.setResult(result);
		}
	}
	
	public GeneratedMessage getLastResult(long requestId, Channel channel) {
		SessionInfo session = sessions.get(channel);
		
		if(session == null) {
			return null;
		}
		
		if(requestId != session.getRequestId()) {
			return null;
		}
		
		return session.getResult();
	}
	
	public SessionInfo getSession(Channel channel) {
		
		return sessions.get(channel);
	}
	
	public long getRequestId(Channel channel) {
		
		SessionInfo session = sessions.get(channel);
		
		if(session == null) {
			return 0;
		}
		
		return session.getRequestId();
	}
	
	public SessionInfo removeSession(Channel channel) {
		
		return sessions.remove(channel);
	}
	
	public void sendMsg(long requestId, byte status, Channel channel, GeneratedMessage proto) {
		
		ResponseContext context = new ResponseContext();
		
		context.setRequestId(requestId);
		context.setStatus(status);
		context.setResult(proto);
		channelMgr.writeAndFlush(channel, context);
	}
	
	public void closeTimeoutSession() {

		for(Map.Entry<Channel, SessionInfo> entry : sessions.entrySet()) {
			
			SessionInfo session = entry.getValue();

			int curTime = SystemTimeUtil.getTimestamp();
			int lastTime = session.getLastPingSec();

			if(curTime - lastTime > heatSecond) {
				System.out.println("服务端会话关闭");
				removeSession(entry.getKey());
				CloseUtil.closeQuietly(session.getChannel());
			}
		}
	}

	
	public boolean pingHandler(Channel channel, long requestId) {
		SessionInfo session = sessions.get(channel);
		
		if(session != null) {
			session.setLastPingSec(SystemTimeUtil.getTimestamp());
		}
		
		p_module_common_ack.Builder builder = p_module_common_ack.newBuilder();
		
		sendMsg(requestId, StatusCode.SUCCESS, channel, builder.build());
		return true;
	}
}
