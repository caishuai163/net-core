package cn.gyyx.core.net.mgr;

import cn.gyyx.core.net.module.SyncContext;
import io.netty.channel.Channel;

public class ClientSessionInfo  {

	private Channel channel;
	
	private SyncContext syncContext;
	
	private String serviceName;
	
	private String remoteIp;
	
	private int remotePort;
	
	private int lastPingSec;

	public SyncContext getSyncContext() {
		return syncContext;
	}

	public void setSyncContext(SyncContext syncContext) {
		this.syncContext = syncContext;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public String getRemoteIp() {
		return remoteIp;
	}

	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public int getLastPingSec() {
		return lastPingSec;
	}

	public void setLastPingSec(int lastPingSec) {
		this.lastPingSec = lastPingSec;
	}



}
