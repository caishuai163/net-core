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
                    /** ProtoBuf处理管理类 */
                    ProtoHandlerMgr protoHandlerMgr = new ProtoHandlerMgr();
                    /** netty reactor 事件组管理类 */
                    EventGroupMgr eventGroupMgr = new EventGroupMgr();
                    /** netty cannel 数据写入操作类 */
                    ChannelWriteMgr channelWriteMgr = new ChannelWriteMgr();
                    /** 定时器管理 */
                    TimerMgr timerMgr = new TimerMgr();
                    /** Zookeeper 管理类 */
                    CuratorMgr curatorMgr = new CuratorMgr();
                    /** 初始化服务注册&发现 */
                    ServiceDiscoverMgr discoverMgr = new ServiceDiscoverMgr(
                            curatorMgr);

                    /** 路由服务 */
                    ServiceRoute serviceRote = new ServiceRoute();
                    /** 服务发现 */
                    ServiceDiscover discover = new ServiceDiscover(curatorMgr,
                            discoverMgr, serviceRote);
                    /** 服务注册 */
                    ServiceRegister serviceRegister = new ServiceRegister(
                            discoverMgr);
                    /** 初始化客户端session管理器 */
                    ClientSessionMgr clientSessionMgr = new ClientSessionMgr(
                            channelWriteMgr, serviceRegister);
                    /** 初始化连接管理器 */
                    ConnectMgr connectmgr = new ConnectMgr(eventGroupMgr,
                            clientSessionMgr, timerMgr, protoHandlerMgr);

                    /** 初始化一个客户生产者 */
                    clientService = new UserClientService(protoHandlerMgr,
                            connectmgr, discover);
                    /** 初始化客户端消费者 */
                    UserClientConsumer consumer = new UserClientConsumer(
                            clientService);
                    /** 启动生产者消费者无锁队列 */
                    ClientNonLockQueue.start(consumer);
                    isStart = true;
                }
            }
        }
    }

    public static p_module_user_login_result send() throws Exception {

        init();
        /** 客户端发送数据 */
        return clientService.accountLogin("pipeline", "1236978");
    }
}
