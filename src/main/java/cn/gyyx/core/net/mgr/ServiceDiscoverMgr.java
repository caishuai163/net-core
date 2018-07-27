package cn.gyyx.core.net.mgr;

import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import cn.gyyx.core.net.service.ServiceEntry;

public class ServiceDiscoverMgr {

	private ServiceDiscovery<ServiceEntry> serviceDiscovery;
	
	private static final String BASE_PATH = "services";
	
	public ServiceDiscoverMgr(CuratorMgr curatorMgr) throws Exception {
		this(curatorMgr, BASE_PATH);
	}
	
	public ServiceDiscoverMgr(CuratorMgr curatorMgr, String basePath) throws Exception {
		JsonInstanceSerializer<ServiceEntry> serializer = new JsonInstanceSerializer<ServiceEntry>(ServiceEntry.class);
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceEntry.class)
                .client(curatorMgr.getCuratorClient())
                .basePath(basePath)
                .serializer(serializer)
                .build();

        serviceDiscovery.start();
	}
	
	public ServiceDiscovery<ServiceEntry> get() {
		return serviceDiscovery;
	}
	
	public void close() {
		CloseableUtils.closeQuietly(serviceDiscovery);
	}
}
