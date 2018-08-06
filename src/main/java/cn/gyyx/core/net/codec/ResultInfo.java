package cn.gyyx.core.net.codec;

import com.google.protobuf.GeneratedMessage;

/**
 * 结果数据
 */
public class ResultInfo {
    /**
     * 错误码
     */
    private byte errorCode;
    /**
     * 错误信息
     */
    private String message;
    /**
     * protobuf 数据
     */
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
