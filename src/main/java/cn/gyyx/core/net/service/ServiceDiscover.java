package cn.gyyx.core.net.service;

import java.util.Map;
import com.google.common.collect.Maps;
import cn.gyyx.core.net.mgr.CuratorMgr;
import cn.gyyx.core.net.mgr.ServiceDiscoverMgr;

import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;

/**
 * 服务发现类
 */
public class ServiceDiscover {

    private ServiceDiscoverMgr serviceDisoverMgr;
    /**
     * 使用 volatile,以便于当缓存发生变换时，及时更新到内存中
     */
    private volatile Map<String, ServiceProvider<ServiceEntry>> providers = Maps
            .newHashMap();

    private ServiceRoute serviceRoute;

    private Object lock = new Object();

    /**
     * 构造一个服务发现类
     * 
     * @param curatorMgr
     *            zookeeperClient管理器(实际上没有初始化到该对象中，该类中也没有对其的任何操作) 详细数据类型见类:
     *            {@link CuratorMgr}
     * @param serviceDisoverMgr
     *            服务发现管理类实例的对象 详细数据类型见类: {@link ServiceDiscoverMgr}
     * @param serviceRoute
     *            路由服务，管理zookeeper服务的路由策略类型 详细数据类型见类: {@link ServiceRoute}
     */
    public ServiceDiscover(CuratorMgr curatorMgr,
            ServiceDiscoverMgr serviceDisoverMgr, ServiceRoute serviceRoute) {
        this.serviceRoute = serviceRoute;
        this.serviceDisoverMgr = serviceDisoverMgr;
    }

    /**
     * 获取指定名称的服务实例
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

        /** 获取服务提供者 */
        ServiceProvider<ServiceEntry> provider = this.getProvider(serviceName);

        /** 设置路由模式 */
        serviceRoute.setProviderStrategryType(strategryType);

        /** 获取实例 */
        return provider.getInstance();
    }

    /**
     * 获取指定名称的服务实例(主要针对固定路由模式指定路由id)
     *
     * @param serviceName
     *            实例名
     * @param strategryType
     *            路由策略模式
     * @param id
     *            指定路由id
     * @return {@link ServiceInstance}
     * @throws Exception
     * 
     */
    public ServiceInstance<ServiceEntry> getService(String serviceName,
            ProviderStrategryType strategryType, int id) throws Exception {
        /** 获取服务提供者 */
        ServiceProvider<ServiceEntry> provider = this.getProvider(serviceName);
        /** 设置路由模式 */
        serviceRoute.setProviderStrategryType(strategryType, id);
        /** 获取实例 */
        return provider.getInstance();
    }

    public void close() {
        /** 循环关闭所有已经缓存的服务提供者 */
        for (Map.Entry<String, ServiceProvider<ServiceEntry>> entry : providers
                .entrySet()) {
            CloseableUtils.closeQuietly(entry.getValue());
        }
        /** 关闭服务发现功能 */
        serviceDisoverMgr.close();
    }

    /**
     * 获取指定名称的服务提供者provider
     *
     * @param serviceName
     *            服务名称
     * @return {@link ServiceProvider}
     * @throws Exception
     * 
     */
    private ServiceProvider<ServiceEntry> getProvider(String serviceName)
            throws Exception {
        /** 缓存map中获取实例名为serverName的实例 */
        ServiceProvider<ServiceEntry> provider = providers.get(serviceName);
        /** 加锁获取实例(lazy单例双重判定) */
        if (provider == null) {
            synchronized (lock) {
                provider = providers.get(serviceName);
                if (provider == null) {
                    /** 获取服务并指定路由策略模式 */
                    provider = serviceDisoverMgr.get().serviceProviderBuilder()
                            .serviceName(serviceName)
                            .providerStrategy(serviceRoute).build();
                    /** 服务启用 唯有启用的服务才能被使用 */
                    provider.start();
                    /** 缓存服务 */
                    providers.put(serviceName, provider);
                }
            }
        }

        return provider;
    }
}
