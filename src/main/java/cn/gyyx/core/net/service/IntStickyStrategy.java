package cn.gyyx.core.net.service;

import java.util.List;

import org.apache.curator.x.discovery.ProviderStrategy;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceProvider;

public class IntStickyStrategy<T> implements ProviderStrategy<T> {

	private int id = 0;
	
	public void setId(int id) {
		this.id = id;
	}
	
    @Override
    public ServiceInstance<T> getInstance(InstanceProvider<T> instanceProvider) throws Exception
    {
        List<ServiceInstance<T>>    instances = instanceProvider.getInstances();
        
        if ( instances.size() == 0 )
        {
            return null;
        }
        
        int thisIndex = id;
        
        return instances.get(thisIndex % instances.size());
    }
}
