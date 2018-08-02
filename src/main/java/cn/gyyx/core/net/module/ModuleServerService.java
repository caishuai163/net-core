package cn.gyyx.core.net.module;

import cn.gyyx.core.net.mgr.SessionInfo;
import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.codec.StatusCode;
import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.mgr.ServerSessionMgr;
import cn.gyyx.core.net.mgr.TcpServerMgr;
import cn.gyyx.core.net.protocol.ProtoType;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ack;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ping;
import cn.gyyx.core.net.queue.EventInfo;
import cn.gyyx.core.net.service.EndPoint;
import cn.gyyx.core.net.service.ServiceEntry;
import cn.gyyx.core.net.service.ServiceRegister;
import cn.gyyx.core.net.util.CloseUtil;
import cn.gyyx.core.net.util.SystemTimeUtil;

/**
 * 服务器端业务实现类
 */
public abstract class ModuleServerService {

    protected ProtoHandlerMgr protohandlerMgr;

    protected TcpServerMgr tcpServerMgr;

    protected ServiceRegister serviceRegister;

    protected ServerSessionMgr serverSessionMgr;

    /**
     * 初始化服务端业务实现service类
     * 
     * @param protohandlerMgr
     * @param tcpServerMgr
     * @param serviceRegister
     * @throws Exception
     */
    public ModuleServerService(ProtoHandlerMgr protohandlerMgr,
            TcpServerMgr tcpServerMgr, ServiceRegister serviceRegister)
            throws Exception {
        this.protohandlerMgr = protohandlerMgr;
        this.tcpServerMgr = tcpServerMgr;
        this.serviceRegister = serviceRegister;
        this.serverSessionMgr = tcpServerMgr.getServerSessionMgr();
    }

    /**
     * 初始化方法
     * 
     * @throws Exception
     */
    public void init() throws Exception {
        registerProtoHandler();
    }

    public ServerSessionMgr getServerSessionMgr() {
        return this.serverSessionMgr;
    }

    /**
     * 启动服务端业务处理server
     *
     * @param ip
     * @param port
     * @throws Exception
     *             void
     */
    public void startServer(String ip, int port) throws Exception {
        EndPoint endpoint = tcpServerMgr.acceptService(ip, port);

        ServiceEntry entry = new ServiceEntry();

        entry.setServiceName(getServiceName());
        entry.setIp(endpoint.getIp());
        entry.setPort(endpoint.getPort());

        serviceRegister.registerService(entry);
    }

    protected abstract String getServiceName();

    /**
     * 初始化向protohandler中注入心跳包的解析映射关系
     * 
     * caishuai 2018年8月1日 下午4:58:22 描述 void
     */
    protected void registerProtoHandler() {
        protohandlerMgr.registerProto(ProtoType.P_MODULE_COMMON_PING,
            p_module_common_ping.class);
        protohandlerMgr.registerProto(ProtoType.P_MODULE_COMMON_ACK,
            p_module_common_ack.class);
        registerProtoHandlerImpl();
    }

    /**
     * 初始化向protohandler中注入心跳包的解析映射关系 子类中可以通过重写该方法注入自己的额外实现
     */
    protected abstract void registerProtoHandlerImpl();

    /**
     * <h3>处理客户端发来的请求</h3></br>
     * <ul>
     * <li>针对同一信道，查询上次请求的信息(相同请求的数据唯一标识ID)。</li>
     * <li>如果查到上次的数据，直接返回数据，不再执行相同的数据请求</li>
     * <li>获取到服务端程序解析内容处理后返回的信息</li>
     * <li>保留该channel的请求的最后一次结果信息并缓存</li>
     * <li>发送结果信息给客户端</li>
     * </ul>
     * 
     * @param data
     *            服务端生产者生产的数据，即客户端请求的数据
     */
    public void onClientProtoCome(EventInfo data) {
        /** 针对同一信道，查询上次请求的信息。 */
        GeneratedMessage result = serverSessionMgr
                .getLastResult(data.getRequestId(), data.getChannel());
        /** 如果查到上次的数据，直接返回数据，不再执行相同的数据请求 */
        if (result != null) {
            /** 服务端发送响应状态 */
            serverSessionMgr.sendMsg(data.getRequestId(), StatusCode.SUCCESS,
                data.getChannel(), result);
            return;
        }

        try {
            /** 获取到服务端程序解析内容处理后返回的信息 */
            result = protohandlerMgr.handleClientProto(data.getRequestId(),
                data.getProtoEnum(), data.getChannel(), data.getBody());
            /** 保留该channel的请求的最后一次结果信息并缓存 */
            serverSessionMgr.saveLastResult(data.getRequestId(),
                data.getChannel(), result);
            /** 发送结果信息给客户端 */
            serverSessionMgr.sendMsg(data.getRequestId(), StatusCode.SUCCESS,
                data.getChannel(), result);
        } catch (Throwable ex) {
            ex.printStackTrace();
            /** 发送异常信息给客户端 */
            serverSessionMgr.sendMsg(data.getRequestId(), StatusCode.EXCEPTION,
                data.getChannel(), result);
        }
    }

    /**
     * 事件是客户端注册的时候，创建一个session信息，将它与channel进行绑定
     * 
     * @param data
     */
    public void onClientRegister(EventInfo data) {

        SessionInfo session = new SessionInfo();

        session.setChannel(data.getChannel());
        session.setLastPingSec(SystemTimeUtil.getTimestamp());

        serverSessionMgr.addSession(session);
    }

    public void onClientDisconnect(EventInfo data) {

        serverSessionMgr.removeSession(data.getChannel());

        CloseUtil.closeQuietly(data.getChannel());
    }

}
