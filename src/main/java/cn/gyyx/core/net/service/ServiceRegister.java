package cn.gyyx.core.net.service;

import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import cn.gyyx.core.net.mgr.ServiceDiscoverMgr;

public class ServiceRegister {

	private final ServiceDiscoverMgr serviceDisoverMgr;

	public ServiceRegister(ServiceDiscoverMgr serviceDisoverMgr) {
        this.serviceDisoverMgr = serviceDisoverMgr;
	}
	
	public void registerService(ServiceEntry serviceEntry) throws Exception {
		ServiceInstance<ServiceEntry> instance = ServiceInstance.<ServiceEntry>builder()
				.id(serviceEntry.address())
				.name(serviceEntry.getServiceName())
				.address(serviceEntry.getIp())
				.port(serviceEntry.getPort())
				.payload(serviceEntry)
				.uriSpec(new UriSpec("{scheme}://{address}:{port}"))
				.build();
		
		serviceDisoverMgr.get().registerService(instance);
    }

    public void unregisterService(ServiceEntry serviceEntry) throws Exception {
    	ServiceInstance<ServiceEntry> instance = ServiceInstance.<ServiceEntry>builder()
    			.id(serviceEntry.address())
				.name(serviceEntry.getServiceName())
				.build();
    	serviceDisoverMgr.get().unregisterService(instance);
    }
    
    public void close()  {
    	serviceDisoverMgr.close();
    }
}
