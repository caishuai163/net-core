package cn.gyyx.core.net.mgr;

import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import cn.gyyx.core.net.service.ServiceEntry;

/**
 * zookeeper服务发现管理类
 */
public class ServiceDiscoverMgr {

    private ServiceDiscovery<ServiceEntry> serviceDiscovery;

    private static final String BASE_PATH = "services";

    /**
     * 构造方法
     * 
     * @see #ServiceDiscoverMgr(CuratorMgr, String)
     * @param curatorMgr
     *            zookeeper信息 {@link CuratorMgr}
     * @throws Exception
     */
    public ServiceDiscoverMgr(CuratorMgr curatorMgr) throws Exception {
        this(curatorMgr, BASE_PATH);
    }

    /**
     * 构造方法,初始化服务注册&发现
     * 
     * @param curatorMgr
     *            zookeeper信息 {@link CuratorMgr}
     * @param basePath
     *            zookeeper 指定路径
     * @throws Exception
     */
    public ServiceDiscoverMgr(CuratorMgr curatorMgr, String basePath)
            throws Exception {
        /** 数据序列化方式 */
        JsonInstanceSerializer<ServiceEntry> serializer = new JsonInstanceSerializer<ServiceEntry>(
                ServiceEntry.class);
        /** 初始化服务注册与发现 */
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceEntry.class)
                .client(curatorMgr.getCuratorClient()).basePath(basePath)
                .serializer(serializer).build();
        /** 服务注册与发现启动 */
        serviceDiscovery.start();
    }

    /**
     * 获取服务注册与发现
     * 
     * @return ServiceDiscovery<ServiceEntry>
     */
    public ServiceDiscovery<ServiceEntry> get() {
        return serviceDiscovery;
    }

    /**
     * 关闭服务注册与发现
     */
    public void close() {
        CloseableUtils.closeQuietly(serviceDiscovery);
    }
}
