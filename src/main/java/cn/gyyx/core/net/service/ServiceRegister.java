package cn.gyyx.core.net.service;

import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import cn.gyyx.core.net.mgr.ServiceDiscoverMgr;

/**
 * 服务注册
 */
public class ServiceRegister {

    private final ServiceDiscoverMgr serviceDisoverMgr;

    /**
     * 构造方法
     * 
     * @param serviceDisoverMgr
     *            传入服务注册和发现管理实现类 {@link ServiceDiscoverMgr}
     */
    public ServiceRegister(ServiceDiscoverMgr serviceDisoverMgr) {
        this.serviceDisoverMgr = serviceDisoverMgr;
    }

    /**
     * 服务注册
     *
     * @param serviceEntry
     * @throws Exception
     *             void
     */
    public void registerService(ServiceEntry serviceEntry) throws Exception {
        ServiceInstance<ServiceEntry> instance = ServiceInstance
                .<ServiceEntry> builder().id(serviceEntry.address())
                .name(serviceEntry.getServiceName())
                .address(serviceEntry.getIp()).port(serviceEntry.getPort())
                .payload(serviceEntry)
                .uriSpec(new UriSpec("{scheme}://{address}:{port}")).build();
        /** 注册他的provider */
        serviceDisoverMgr.get().registerService(instance);
    }

    public void unregisterService(ServiceEntry serviceEntry) throws Exception {
        ServiceInstance<ServiceEntry> instance = ServiceInstance
                .<ServiceEntry> builder().id(serviceEntry.address())
                .name(serviceEntry.getServiceName()).build();
        serviceDisoverMgr.get().unregisterService(instance);
    }

    public void close() {
        serviceDisoverMgr.close();
    }
}
