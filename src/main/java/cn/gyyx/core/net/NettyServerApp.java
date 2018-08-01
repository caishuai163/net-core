package cn.gyyx.core.net;

import cn.gyyx.core.net.mgr.ChannelWriteMgr;
import cn.gyyx.core.net.mgr.CuratorMgr;
import cn.gyyx.core.net.mgr.EventGroupMgr;
import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.mgr.ServerSessionMgr;
import cn.gyyx.core.net.mgr.ServiceDiscoverMgr;
import cn.gyyx.core.net.mgr.TcpServerMgr;
import cn.gyyx.core.net.mgr.TimerMgr;
import cn.gyyx.core.net.module.user.UserConsumer;
import cn.gyyx.core.net.module.user.UserService;
import cn.gyyx.core.net.queue.NonLockQueue;
import cn.gyyx.core.net.service.ServiceRegister;

public class NettyServerApp {

    public static void main(String[] args) throws Exception {

        System.out.println("netty服务端开始启动");

        ProtoHandlerMgr protoHandlerMgr = new ProtoHandlerMgr();

        EventGroupMgr eventGroupMgr = new EventGroupMgr();
        ChannelWriteMgr channelWriteMgr = new ChannelWriteMgr();
        ServerSessionMgr serverSessionMgr = new ServerSessionMgr(
                channelWriteMgr);
        TimerMgr timerMgr = new TimerMgr();
        TcpServerMgr tcpServerMgr = new TcpServerMgr(eventGroupMgr,
                serverSessionMgr, timerMgr, protoHandlerMgr);

        CuratorMgr curatorMgr = new CuratorMgr();
        ServiceDiscoverMgr discoverMgr = new ServiceDiscoverMgr(curatorMgr);
        ServiceRegister serverRegister = new ServiceRegister(discoverMgr);

        /** 实例化服务端具体业务实现service */
        UserService userService = new UserService(protoHandlerMgr, tcpServerMgr,
                serverRegister);
        /**
         * 创建消费者， 启动服务端disrupter无锁队列
         */
        NonLockQueue.start(new UserConsumer(userService), args[0],
            Integer.valueOf(args[1]));

    }
}
