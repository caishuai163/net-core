package cn.gyyx.core.net.mgr;


import com.google.protobuf.GeneratedMessage;

import io.netty.channel.Channel;

public class SessionInfo {

	private long requestId;
	
	private Channel channel;
	
	private int lastPingSec;
	
	private GeneratedMessage result;

	public SessionInfo() {

	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public int getLastPingSec() {
		return lastPingSec;
	}
	
	public void setLastPingSec(int lastPingSec) {
		this.lastPingSec = lastPingSec;
	}

	public long getRequestId() {
		return requestId;
	}

	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}

	public GeneratedMessage getResult() {
		return result;
	}

	public void setResult(GeneratedMessage result) {
		this.result = result;
	}

}
