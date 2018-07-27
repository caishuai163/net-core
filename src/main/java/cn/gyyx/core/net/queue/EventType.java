package cn.gyyx.core.net.queue;

public enum EventType {

	 /**
     * 收到服务器发过来的协议包
     */
    SERVER_PROTO_COMMING,
    
    /**
     * 客户端发过来的协议包
     */
    CLIENT_PROTO_COMMING,
    
    /**
     * 客户端发送
     */
    CLIENT_SEND,
    
    /**
     * 客户端注册
     */
    CLIENT_REGISTER,
    
    /**
     * 客户端关闭连接
     */
    CLIENT_DISCONNECT,
    
    /**
     * 服务器关闭连接
     */
    SERVER_DISCONNECT
}
