package cn.gyyx.core.net.mgr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.gyyx.core.net.codec.ClientEncoder;
import cn.gyyx.core.net.codec.ServerDecoder;
import cn.gyyx.core.net.timer.DefaultTimer;
import cn.gyyx.core.net.timer.TimerCallBack;
import cn.gyyx.core.net.util.SystemTimeUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * <h3>连接管理器</h3>
 * 
 */
public class ConnectMgr {

    /** 事件组管理器 */
    private EventGroupMgr eventGroupMgr;

    /** 客户端session管理器 */
    private ClientSessionMgr clientSessionMgr;
    /** 定时任务调度 */
    private TimerMgr timerMgr;
    /** ProtoBuf处理管理类 */
    private ProtoHandlerMgr protoHandlerMgr;

    /** 重连超时时间 */
    private int tryReconnectTimeout = 600000;
    /** 重连列表list */
    private List<ReconnectInfo> reconnects = new ArrayList<>();

    /**
     * 构造一个连接管理器
     * 
     * @param eventGroupMgr
     * @param clientSessionMgr
     * @param timerMgr
     * @param protoHandlerMgr
     */
    public ConnectMgr(EventGroupMgr eventGroupMgr,
            ClientSessionMgr clientSessionMgr, TimerMgr timerMgr,
            ProtoHandlerMgr protoHandlerMgr) {
        this.eventGroupMgr = eventGroupMgr;
        this.clientSessionMgr = clientSessionMgr;
        this.timerMgr = timerMgr;
        /** 定时器中增加 客户端发送ping请求的定时任务 */
        this.timerMgr.add(new DefaultTimer(clientSessionMgr::sendPing));
        /** 定时器中增加 客户端检查超时连接的定时任务 */
        this.timerMgr
                .add(new DefaultTimer(clientSessionMgr::checkTimeoutSession));
        /** 定时器中增加 客户端重连的定时任务 */
        this.timerMgr.add(new DefaultTimer(this::reConnect, 5000));
        this.protoHandlerMgr = protoHandlerMgr;
    }

    /**
     * 获取当前连接的客户端session管理器
     * 
     * @return {@link ClientSessionMgr}
     */
    public ClientSessionMgr getClientSessionMgr() {
        return this.clientSessionMgr;
    }

    /**
     * 获取当前连接的ProtoBuf处理管理类
     * 
     * @return {@link ProtoHandlerMgr}
     */
    public ProtoHandlerMgr getProtoHandlerMgr() {
        return this.protoHandlerMgr;
    }

    /**
     * <h3>建立一个netty通道</h3>
     * 
     * @see #doConnect(String, int)
     * @param ip
     *            IP地址
     * @param port
     *            端口号
     * @return {@link Channel}
     */
    public Channel connect(String ip, int port) {

        return doConnect(ip, port);
    }

    /**
     * <h3>建立一个netty通道</h3>
     * 
     * @see #doConnect(String, int)
     * @param serviceName
     *            业务名称
     * @param ip
     *            IP地址
     * @param port
     *            端口号
     * @return {@link Channel}
     */
    public Channel connect(String serviceName, String ip, int port) {

        try {
            String address = ip + ":" + port;
            Channel channel = clientSessionMgr.getChannel(serviceName, address);
            if (channel != null) {
                return channel;
            }

            /** 如果缓存中不存在 直接创建连接netty Channel */
            channel = doConnect(ip, port);
            /** 缓存channel */
            if (channel != null) {
                ClientSessionInfo session = new ClientSessionInfo();
                session.setServiceName(serviceName);
                session.setChannel(channel);
                session.setRemoteIp(ip);
                session.setRemotePort(port);
                session.setLastPingSec(SystemTimeUtil.getTimestamp());
                clientSessionMgr.addSession(serviceName, address, session);
            }

            return channel;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    public void onDisconnect(Channel channel) {

        if (clientSessionMgr.getSession(channel) != null) {
            ReconnectInfo reconnectInfo = new ReconnectInfo();
            reconnectInfo.setChannel(channel);
            reconnectInfo.setStartTime(SystemTimeUtil.getTimestamp());
            reconnects.add(reconnectInfo);
        }
    }

    private void reConnect() {

        for (Iterator<ReconnectInfo> itr = reconnects.iterator(); itr
                .hasNext();) {

            ReconnectInfo info = itr.next();
            long endTime = SystemTimeUtil.getTimestamp();
            if (endTime - info.getStartTime() > tryReconnectTimeout) {
                itr.remove();
                return;
            }

            ClientSessionInfo session = clientSessionMgr
                    .getSession(info.getChannel());

            if (session == null) {
                itr.remove();
                return;
            }

            Channel newchannel = doConnect(session.getRemoteIp(),
                session.getRemotePort());

            if (newchannel != null) {
                itr.remove();
                clientSessionMgr.removeSession(info.getChannel());

                String address = session.getRemoteIp() + ":"
                        + session.getRemotePort();

                ClientSessionInfo newSession = new ClientSessionInfo();
                newSession.setServiceName(session.getServiceName());
                newSession.setChannel(newchannel);
                newSession.setRemoteIp(session.getRemoteIp());
                newSession.setRemotePort(session.getRemotePort());
                newSession.setLastPingSec(SystemTimeUtil.getTimestamp());
                clientSessionMgr.addSession(session.getServiceName(), address,
                    newSession);
            }
        }
    }

    /**
     * <h3>建立一个netty通道</h3> </br>
     * <h5>关于netty channel</h5>
     * <ul>
     * <li>1）通道状态主要包括：打开、关闭、连接</li>
     * <li>2）通道主要的IO操作，读(read)、写(write)、连接(connect)、绑定(bind)。</li>
     * <li>3）所有的IO操作都是异步的，调用诸如read,write方法后，并不保证IO操作完成，但会返回一个凭证，在IO操作成功，取消或失败后会记录在该凭证中。</li>
     * <li>4）channel有父子关系，SocketChannel是通过ServerSocketChannel接受创建的，故SocketChannel的parent()方法返回的就是ServerSocketChannel。</li>
     * <li>5）在Channel使用完毕后，请调用close方法，释放通道占用的资源。</li>
     * </ul>
     * 
     * @param ip
     *            IP地址
     * @param port
     *            端口号
     * @return {@link Channel}
     */
    private Channel doConnect(String ip, int port) {
        /** netty channel 启动器 */
        Bootstrap bootstrap = new Bootstrap();
        /** 设置模式为nio */
        bootstrap.channel(NioSocketChannel.class);
        /** 设置处理事件的事件组 */
        bootstrap.group(eventGroupMgr.getWorkGroup());

        /** 初始化handler事件 */
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ServerDecoder(ConnectMgr.this));
                ch.pipeline().addLast(new ClientEncoder(ConnectMgr.this));
            }
        });
        /** 启动配置 TODO */
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_RCVBUF, 64 * 1024)
                .option(ChannelOption.SO_SNDBUF, 64 * 1024)
                .option(ChannelOption.SO_LINGER, 0);
        /** 建立连接平返回channel */
        try {
            return bootstrap.connect(ip, port).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

}
