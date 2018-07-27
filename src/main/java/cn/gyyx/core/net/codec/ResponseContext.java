package cn.gyyx.core.net.codec;

import com.google.protobuf.GeneratedMessage;

public class ResponseContext {

	private long requestId;
	
	private byte status;
	
	private GeneratedMessage result;
	
	public ResponseContext() {
		
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

	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}
}
