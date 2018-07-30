package cn.gyyx.core.net.module;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.x.discovery.ServiceInstance;

import java.util.Map;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.codec.ResultInfo;
import cn.gyyx.core.net.codec.StatusCode;
import cn.gyyx.core.net.mgr.ClientSessionMgr;
import cn.gyyx.core.net.mgr.ConnectMgr;
import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.protocol.ProtoType;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ack;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ping;
import cn.gyyx.core.net.queue.ClientEventInfo;
import cn.gyyx.core.net.queue.ClientNonLockQueue;
import cn.gyyx.core.net.service.ProviderStrategryType;
import cn.gyyx.core.net.service.ServiceDiscover;
import cn.gyyx.core.net.service.ServiceEntry;
import io.netty.channel.Channel;

/**
 * <p>
 * 客户端业务处理service抽象类
 * </p>
 * 
 */
public abstract class ModuleClientService {

    protected ProtoHandlerMgr protohandlerMgr;

    protected ConnectMgr connectMgr;

    private ClientSessionMgr clientSessionMgr;

    private ServiceDiscover discover;

    private static final int DEFALT_TIMEOUT = 30;
    /**
     * 一个线程安全的hashmap
     */
    private Map<Long, SyncContext> syncContexts = new ConcurrentHashMap<>();
    /**
     * 从0开始的线程安全的long型数字,被用于同步的标识
     */
    private AtomicLong syncGuid = new AtomicLong(0);

    /**
     * <p>
     * 客户端业务处理service构造方法
     * </p>
     * 初始化对象
     * <ul>
     * <li>{@link #protohandlerMgr}</li>
     * <li>{@link #connectMgr}</li>
     * <li>{@link #clientSessionMgr}</li>
     * <li>{@link #discover}</li>
     * </ul>
     * 
     * @param protohandlerMgr
     *            {@linkplain ProtoHandlerMgr}
     * @param connectMgr
     * @param discover
     */
    public ModuleClientService(ProtoHandlerMgr protohandlerMgr,
            ConnectMgr connectMgr, ServiceDiscover discover) {
        this.protohandlerMgr = protohandlerMgr;
        this.connectMgr = connectMgr;
        this.clientSessionMgr = connectMgr.getClientSessionMgr();
        this.discover = discover;
    }

    /**
     * 获取同步的内容
     *
     * @param syncId
     *            同步的id
     * @return SyncContext
     */
    public SyncContext getSyncContext(long syncId) {
        return this.syncContexts.get(syncId);
    }

    /**
     * 
     * <p>
     * 同步发送消息（有？？？？？？？？？？？） TODO
     * </p>
     *
     *
     * @param serviceName
     *            业务名称
     * @param proto
     *            protoBuf数据
     * @param strategryType
     * @return ResultInfo
     * @throws InterruptedException
     * 
     */
    public ResultInfo sendSyncMsg(String serviceName, GeneratedMessage proto,
            ProviderStrategryType strategryType) throws InterruptedException {
        /** 获取递增的标识位(线程安全操作原序列+1并返回新值) */
        long id = syncGuid.incrementAndGet();

        /** 设置new一个同步内容bean */
        SyncContext context = new SyncContext();

        /**
         * CountDownLatch是通过一个计数器来实现的，计数器的初始值为线程的数量。</br>
         * 每当一个线程完成了自己的任务后，计数器的值就会减1。</br>
         * 当计数器值到达0时，它表示所有的线程已经完成了任务，然后在闭锁上等待的线程就可以恢复执行任务。
         */
        CountDownLatch latch = new CountDownLatch(1);

        /** 设置标志位和计数的锁 */
        context.setLatch(latch);
        context.setSyncId(id);

        /** new 一个客户端事件 */
        ClientEventInfo eventInfo = new ClientEventInfo();

        /**
         * 设置唯一标识,业务名称,protobuf数据 ,strategry(策略？)类型
         */
        eventInfo.setId(id);
        eventInfo.setServiceName(serviceName);
        eventInfo.setBody(proto);
        eventInfo.setStrategryType(strategryType);

        /** 缓存context数据 */
        syncContexts.put(id, context);
        /** 发布客户端事件 */
        ClientNonLockQueue.publish(eventInfo);

        try {
            /** 阻塞休眠,当 latch的状态值为0时，继续执行 */
            latch.await();
            /** 返回结果 */
            return context.getResult();
        } finally {
            /**
             * 移除缓存context数据
             */
            syncContexts.remove(id);
        }
    }

    /**
     * 客户端发送事件业务流程
     * 
     * @param data
     *            type: {@link ClientEventInfo}
     * @param entry
     *            type: {@link ServiceEntry}
     * @return {@link ResultInfo}
     * @throws InterruptedException
     * 
     */
    public ResultInfo onClientSend(ClientEventInfo data, ServiceEntry entry)
            throws InterruptedException {

        /**
         * CountDownLatch是通过一个计数器来实现的，计数器的初始值为线程的数量。</br>
         * 每当一个线程完成了自己的任务后，计数器的值就会减1。</br>
         * 当计数器值到达0时，它表示所有的线程已经完成了任务，然后在闭锁上等待的线程就可以恢复执行任务。
         */
        CountDownLatch latch = new CountDownLatch(1);

        /**
         * 缓存数据
         */
        SyncContext context = new SyncContext();
        context.setSyncId(data.getId());
        context.setLatch(latch);

        /** 建立一个netty通道 */
        Channel channel = connectMgr.connect(data.getServiceName(),
            entry.getIp(), entry.getPort());
        /** 如果建立连接失败 返回连接异常 */
        if (channel == null) {
            ResultInfo result = new ResultInfo();
            result.setErrorCode(StatusCode.CONNECTEXCEPTION);
            return result;
        }

        clientSessionMgr.updateSyncContext(context, channel);
        clientSessionMgr.sendMsg(channel, data.getBody());

        /** 如果整个流程30秒内仍没有被释放掉，返回timeOut */
        if (!latch.await(DEFALT_TIMEOUT, TimeUnit.SECONDS)) {
            ResultInfo result = new ResultInfo();
            result.setErrorCode(StatusCode.TIMEOUT);
            return result;
        }

        return context.getResult();

    }

    /**
     * 获取业务所对应的详细信息
     * 
     * @param serviceName
     *            业务名称
     * @param strategryType
     *            策略类型 {@link ProviderStrategryType}
     * @return {@link ServiceEntry}
     */
    public ServiceEntry getServiceAddress(String serviceName,
            ProviderStrategryType strategryType) {
        try {
            /** 获取业务实例 */
            ServiceInstance<ServiceEntry> instance = discover
                    .getService(serviceName, strategryType);

            if (instance == null) {
                return null;
            }

            /** 返回业务实例详情 */
            return instance.getPayload();

        } catch (Throwable ex) {
            ex.printStackTrace();
            return null;
        }

    }

    /**
     * <p>
     * 初始化方法
     * </p>
     * register(or means init) protoBufHandler.More see
     * {@link #registerProto()}</br>
     */
    public void init() {

        registerProto();
    }

    /**
     * 
     * <p>
     * 注册protoBuff处理业务
     * </p>
     * protoBuff处理业务步骤
     * <ul>
     * <li>注册ping 注册ack (心跳包) {@link ProtoHandlerMgr#registerProto(int, Class)}</li>
     * <li>自身其他业务注册实现{@link #registerProtoImpl()}</li>
     * </ul>
     */
    protected void registerProto() {
        protohandlerMgr.registerProto(ProtoType.P_MODULE_COMMON_PING,
            p_module_common_ping.class);
        protohandlerMgr.registerProto(ProtoType.P_MODULE_COMMON_ACK,
            p_module_common_ack.class);
        registerProtoImpl();
    }

    /**
     * 自身其他业务注册实现抽象方法
     */
    protected abstract void registerProtoImpl();
}
