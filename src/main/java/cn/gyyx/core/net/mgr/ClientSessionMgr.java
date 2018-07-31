package cn.gyyx.core.net.mgr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.gyyx.core.net.codec.ResultInfo;
import cn.gyyx.core.net.module.SyncContext;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ping;
import cn.gyyx.core.net.service.ServiceEntry;
import cn.gyyx.core.net.service.ServiceRegister;
import cn.gyyx.core.net.util.CloseUtil;
import cn.gyyx.core.net.util.SystemTimeUtil;
import io.netty.channel.Channel;

public class ClientSessionMgr {
    /**
     * 缓存channels信息</br>
     * 每个serviceName对应的channels </br>
     * 每个channels中每个ip+端口对应的channel
     */
    private Map<String, Map<String, Channel>> serviceChannels = new ConcurrentHashMap<>();

    private ChannelWriteMgr channelMgr;
    private ServiceRegister serviceRegister;
    /**
     * 缓存客户端session信息</br>
     * 每个channel所对应的channel
     */
    private Map<Channel, ClientSessionInfo> sessions = new ConcurrentHashMap<>();
    private int heatSecond = 600000;

    public ClientSessionMgr(ChannelWriteMgr channelMgr,
            ServiceRegister serviceRegister) {
        this.channelMgr = channelMgr;
        this.serviceRegister = serviceRegister;
    }

    /**
     * 缓存中新增channel
     *
     * @param serviceName
     *            业务名
     * @param address
     *            IP地址+port
     * @param session
     *            客户端session
     */
    public void addSession(String serviceName, String address,
            ClientSessionInfo session) {
        /** TODO 缓存session */
        sessions.put(session.getChannel(), session);

        /** 查找channels的map里面是否已经有了这个业务名称的channels */
        Map<String, Channel> channels = serviceChannels.get(serviceName);

        if (channels != null) {
            /** 如果已经有了这个业务名的channels，直接网这个业务的channels里面放新的channel */
            channels.putIfAbsent(address, session.getChannel());
        } else {
            /** 如果没有这个业务名的channels，new一个新的channels */
            Map<String, Channel> newChannels = new ConcurrentHashMap<>();
            /** 里面放新的channel */
            newChannels.put(address, session.getChannel());

            /** 并将channels放入channels的map中 */
            serviceChannels.putIfAbsent(serviceName, newChannels);
        }
    }

    public ClientSessionInfo getSession(Channel channel) {
        return sessions.get(channel);
    }

    public long getRequestId(Channel channel) {

        ClientSessionInfo session = sessions.get(channel);

        SyncContext context = session.getSyncContext();

        return context.getSyncId();
    }

    /**
     * 缓存中获取channel
     * <ul>
     * <li>缓存channels的map中获取对应业务名的channels</li>
     * <li>channels中获取对应address的channel</li>
     * </ul>
     * 
     * @param serviceName
     *            业务名
     * @param address
     *            ip+port
     * @return {@link Channel}
     */
    public Channel getChannel(String serviceName, String address) {

        Map<String, Channel> channels = serviceChannels.get(serviceName);

        if (channels == null) {
            return null;
        }

        return channels.get(address);
    }

    public void removeSession(Channel channel) {

        try {
            ClientSessionInfo session = getSession(channel);

            if (session != null) {
                sessions.remove(channel);

                Map<String, Channel> channels = serviceChannels
                        .get(session.getServiceName());

                if (channels != null) {

                    String address = session.getRemoteIp() + ":"
                            + session.getRemotePort();

                    Channel tmpChannel = channels.get(address);

                    if (tmpChannel != null && tmpChannel == channel) {
                        channels.remove(address);
                    }
                }
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 更新使用的channel的session中的context信息
     *
     * @param syncContext
     * @param channel
     * @return void
     */
    public void updateSyncContext(SyncContext syncContext, Channel channel) {

        ClientSessionInfo session = sessions.get(channel);

        if (session != null) {
            session.setSyncContext(syncContext);
        }
    }

    public void sendPing() {
        for (Map.Entry<Channel, ClientSessionInfo> entry : sessions
                .entrySet()) {

            p_module_common_ping.Builder builder = p_module_common_ping
                    .newBuilder();

            sendMsg(entry.getKey(), builder.build());
        }
    }

    public void checkTimeoutSession() {
        for (Map.Entry<Channel, ClientSessionInfo> entry : sessions
                .entrySet()) {

            ClientSessionInfo session = entry.getValue();

            int curTime = SystemTimeUtil.getTimestamp();
            int lastTime = session.getLastPingSec();

            if (curTime - lastTime > heatSecond) {

                Map<String, Channel> channels = serviceChannels
                        .get(session.getServiceName());

                if (channels != null) {

                    ServiceEntry serviceEntry = new ServiceEntry();

                    serviceEntry.setServiceName(session.getServiceName());
                    serviceEntry.setIp(session.getRemoteIp());
                    serviceEntry.setPort(session.getRemotePort());

                    try {
                        serviceRegister.unregisterService(serviceEntry);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("客户端会话关闭");
                removeSession(entry.getKey());
                CloseUtil.closeQuietly(session.getChannel());
            }
        }
    }

    public void onServerProtoCome(long requestId, Channel channel,
            ResultInfo result) {
        ClientSessionInfo sessionInfo = getSession(channel);

        if (sessionInfo == null) {
            return;
        }

        SyncContext context = sessionInfo.getSyncContext();

        if (context == null) {
            return;
        }

        if (context.getSyncId() != requestId) {
            return;
        }

        context.setResult(result);
    }

    /**
     * 通过channel发送protobuf数据
     *
     * @param channel
     *            netty channel
     * @param proto
     *            protobuf数据
     */
    public void sendMsg(Channel channel, Object proto) {

        channelMgr.writeAndFlush(channel, proto);
    }

    public void pingHandler(Channel channel) {
        ClientSessionInfo session = sessions.get(channel);

        if (session != null) {
            session.setLastPingSec(SystemTimeUtil.getTimestamp());
        }
    }

}
