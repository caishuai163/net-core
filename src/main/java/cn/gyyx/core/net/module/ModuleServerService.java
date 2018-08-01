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

    public void onClientProtoCome(EventInfo data) {

        GeneratedMessage result = serverSessionMgr
                .getLastResult(data.getRequestId(), data.getChannel());

        if (result != null) {
            serverSessionMgr.sendMsg(data.getRequestId(), StatusCode.SUCCESS,
                data.getChannel(), result);
            return;
        }

        try {
            result = protohandlerMgr.handleClientProto(data.getRequestId(),
                data.getProtoEnum(), data.getChannel(), data.getBody());
            serverSessionMgr.saveLastResult(data.getRequestId(),
                data.getChannel(), result);
            serverSessionMgr.sendMsg(data.getRequestId(), StatusCode.SUCCESS,
                data.getChannel(), result);
        } catch (Throwable ex) {
            ex.printStackTrace();
            serverSessionMgr.sendMsg(data.getRequestId(), StatusCode.EXCEPTION,
                data.getChannel(), result);
        }
    }

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
