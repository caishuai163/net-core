package cn.gyyx.core.net;

import cn.gyyx.core.net.mgr.ChannelWriteMgr;
import cn.gyyx.core.net.mgr.ClientSessionMgr;
import cn.gyyx.core.net.mgr.ConnectMgr;
import cn.gyyx.core.net.mgr.CuratorMgr;
import cn.gyyx.core.net.mgr.EventGroupMgr;
import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.mgr.ServiceDiscoverMgr;
import cn.gyyx.core.net.mgr.TimerMgr;
import cn.gyyx.core.net.module.user.client.UserClientConsumer;
import cn.gyyx.core.net.module.user.client.UserClientService;
import cn.gyyx.core.net.protocol.p_module_user.p_module_user_login_result;
import cn.gyyx.core.net.queue.ClientNonLockQueue;
import cn.gyyx.core.net.service.ServiceDiscover;
import cn.gyyx.core.net.service.ServiceRegister;
import cn.gyyx.core.net.service.ServiceRoute;

public class ClientTest {

    private volatile static boolean isStart = false;

    private static Object lockObj = new Object();

    private static UserClientService clientService;

    public static void init() throws Exception {
        if (!isStart) {
            synchronized (lockObj) {
                if (!isStart) {
                    ProtoHandlerMgr protoHandlerMgr = new ProtoHandlerMgr();

                    EventGroupMgr eventGroupMgr = new EventGroupMgr();
                    ChannelWriteMgr channelWriteMgr = new ChannelWriteMgr();

                    TimerMgr timerMgr = new TimerMgr();
                    CuratorMgr curatorMgr = new CuratorMgr();
                    ServiceDiscoverMgr discoverMgr = new ServiceDiscoverMgr(
                            curatorMgr);

                    ServiceRoute serviceRote = new ServiceRoute();
                    ServiceDiscover discover = new ServiceDiscover(curatorMgr,
                            discoverMgr, serviceRote);
                    ServiceRegister serviceRegister = new ServiceRegister(
                            discoverMgr);
                    ClientSessionMgr clientSessionMgr = new ClientSessionMgr(
                            channelWriteMgr, serviceRegister);
                    ConnectMgr connectmgr = new ConnectMgr(eventGroupMgr,
                            clientSessionMgr, timerMgr, protoHandlerMgr);

                    clientService = new UserClientService(protoHandlerMgr,
                            connectmgr, discover);

                    UserClientConsumer consumer = new UserClientConsumer(
                            clientService);
                    ClientNonLockQueue.start(consumer);
                    isStart = true;
                }
            }
        }
    }

    public static p_module_user_login_result send() throws Exception {

        init();
        return clientService.accountLogin("pipeline", "1236978");
    }
}
