package cn.gyyx.core.net.mgr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.codec.ResponseContext;
import cn.gyyx.core.net.codec.StatusCode;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ack;
import cn.gyyx.core.net.util.CloseUtil;
import cn.gyyx.core.net.util.SystemTimeUtil;
import io.netty.channel.Channel;

public class ServerSessionMgr {

    private Map<Channel, SessionInfo> sessions = new ConcurrentHashMap<>();

    private int heatSecond = 600000;

    protected ChannelWriteMgr channelMgr;

    public ServerSessionMgr(ChannelWriteMgr channelMgr) {
        this.channelMgr = channelMgr;
    }

    /**
     * 添加session与channel的缓存信息
     * 
     * @param session
     */
    public void addSession(SessionInfo session) {

        sessions.put(session.getChannel(), session);
    }

    public void saveLastResult(long requestId, Channel channel,
            GeneratedMessage result) {

        SessionInfo session = sessions.get(channel);

        if (session != null) {
            session.setRequestId(requestId);
            session.setResult(result);
        }
    }

    /**
     * <h3>针对同一信道，查询上次请求的信息。</h3> 判断请求id，若一致，证明业务已经在最近一次处理过了这个请求，直接返回最近一次请求的结果
     * 
     * @param requestId
     *            请求数据的唯一标识
     * @param channel
     *            建立的信道，通道
     * @return {@link GeneratedMessage} 最近一次相同请求的内容，若不是相同请求，则返回null
     */
    public GeneratedMessage getLastResult(long requestId, Channel channel) {
        SessionInfo session = sessions.get(channel);

        if (session == null) {
            return null;
        }

        if (requestId != session.getRequestId()) {
            return null;
        }

        return session.getResult();
    }

    public SessionInfo getSession(Channel channel) {

        return sessions.get(channel);
    }

    public long getRequestId(Channel channel) {

        SessionInfo session = sessions.get(channel);

        if (session == null) {
            return 0;
        }

        return session.getRequestId();
    }

    public SessionInfo removeSession(Channel channel) {

        return sessions.remove(channel);
    }

    /**
     * 服务端发送响应状态
     * 
     * @param requestId
     *            请求ID
     * @param status
     *            状态值
     * @param channel
     *            通道
     * @param proto
     *            响应数据
     */
    public void sendMsg(long requestId, byte status, Channel channel,
            GeneratedMessage proto) {

        ResponseContext context = new ResponseContext();

        context.setRequestId(requestId);
        context.setStatus(status);
        context.setResult(proto);
        channelMgr.writeAndFlush(channel, context);
    }

    /**
     * <h3>关闭超时session</h3>
     * <ul>
     * <li>循环缓存sessions中的每一个session</li>
     * <li>获取session的最后一次接收到客户端ping请求的时间与当前时间做差值</li>
     * <li>若得到的差值大于预设值{@link #heatSecond},缓存中移除session,并关闭session channel</li>
     * </ul>
     * 
     */
    public void closeTimeoutSession() {

        for (Map.Entry<Channel, SessionInfo> entry : sessions.entrySet()) {

            SessionInfo session = entry.getValue();

            int curTime = SystemTimeUtil.getTimestamp();
            int lastTime = session.getLastPingSec();

            if (curTime - lastTime > heatSecond) {
                System.out.println("服务端会话关闭");
                removeSession(entry.getKey());
                CloseUtil.closeQuietly(session.getChannel());
            }
        }
    }

    /**
     * 响应客户端的心跳包PIng
     * 
     * @param channel
     *            信道
     * @param requestId
     *            请求ID
     * @return
     */
    public boolean pingHandler(Channel channel, long requestId) {
        /** 获取channel中的session信息 */
        SessionInfo session = sessions.get(channel);

        if (session != null) {
            /** 设置session的最后一次接收到ping的时间 */
            session.setLastPingSec(SystemTimeUtil.getTimestamp());
        }
        /**
         * 反向发送ack数据
         */
        p_module_common_ack.Builder builder = p_module_common_ack.newBuilder();

        sendMsg(requestId, StatusCode.SUCCESS, channel, builder.build());
        return true;
    }
}
