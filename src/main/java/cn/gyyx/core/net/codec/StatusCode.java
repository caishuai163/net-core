package cn.gyyx.core.net.codec;

/**
 * 响应的错误类型
 */
public class StatusCode {

    public static final byte SUCCESS = 1;

    public static final byte EXCEPTION = -1;

    public static final byte SIGNERROR = -2;

    public static final byte TIMEOUT = -3;

    public static final byte CONNECTEXCEPTION = -4;

    public static final byte CONNECTIONCLOSED = -5;
}
