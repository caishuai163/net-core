package cn.gyyx.core.net.codec;

import com.google.protobuf.GeneratedMessage;

public class ResultInfo {

	private byte errorCode;
	
	private String message;
	
	private GeneratedMessage data;

	public byte getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(byte errorCode) {
		this.errorCode = errorCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public GeneratedMessage getData() {
		return data;
	}

	public void setData(GeneratedMessage data) {
		this.data = data;
	}
	
}
