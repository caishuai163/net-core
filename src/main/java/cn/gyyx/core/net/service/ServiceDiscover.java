package cn.gyyx.core.net.service;

import java.util.Map;
import com.google.common.collect.Maps;
import cn.gyyx.core.net.mgr.CuratorMgr;
import cn.gyyx.core.net.mgr.ServiceDiscoverMgr;

import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;

/**
 * <p>
 * ServiceDiscover描述
 * </p>
 * 
 * @author caishuai
 * @since 0.0.1
 */
public class ServiceDiscover {

    private ServiceDiscoverMgr serviceDisoverMgr;

    private volatile Map<String, ServiceProvider<ServiceEntry>> providers = Maps
            .newHashMap();

    private ServiceRoute serviceRoute;

    private Object lock = new Object();

    public ServiceDiscover(CuratorMgr curatorMgr,
            ServiceDiscoverMgr serviceDisoverMgr, ServiceRoute serviceRoute) {
        this.serviceRoute = serviceRoute;
        this.serviceDisoverMgr = serviceDisoverMgr;
    }

    /**
     * 获取服务实例
     *
     * @param serviceName
     *            实例名
     * @param strategryType
     *            路由策略模式
     * @return {@link ServiceInstance}
     * @throws Exception
     * 
     */
    public ServiceInstance<ServiceEntry> getService(String serviceName,
            ProviderStrategryType strategryType) throws Exception {

        ServiceProvider<ServiceEntry> provider = this.getProvider(serviceName,
            strategryType);

        serviceRoute.setProviderStrategryType(strategryType);

        return provider.getInstance();
    }

    public ServiceInstance<ServiceEntry> getService(String serviceName,
            ProviderStrategryType strategryType, int id) throws Exception {

        ServiceProvider<ServiceEntry> provider = this.getProvider(serviceName,
            strategryType);

        serviceRoute.setProviderStrategryType(strategryType, id);

        return provider.getInstance();
    }

    public void close() {

        for (Map.Entry<String, ServiceProvider<ServiceEntry>> entry : providers
                .entrySet()) {
            CloseableUtils.closeQuietly(entry.getValue());
        }

        serviceDisoverMgr.close();
    }

    private ServiceProvider<ServiceEntry> getProvider(String serviceName,
            ProviderStrategryType strategryType) throws Exception {
        /** 缓存map中获取实例名为serverName的实例 */
        ServiceProvider<ServiceEntry> provider = providers.get(serviceName);
        /** 加锁获取实例 */
        if (provider == null) {
            synchronized (lock) {
                provider = providers.get(serviceName);
                if (provider == null) {
                    /** 获取服务注册与发现功能，并获取其中 */
                    provider = serviceDisoverMgr.get().serviceProviderBuilder()
                            .serviceName(serviceName)
                            .providerStrategy(serviceRoute).build();
                    provider.start();
                    providers.put(serviceName, provider);
                }
            }
        }

        return provider;
    }
}
