package cn.gyyx.core.net.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.curator.x.discovery.ProviderStrategy;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceProvider;
import org.apache.curator.x.discovery.strategies.RandomStrategy;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;

public class ServiceRoute implements ProviderStrategy<ServiceEntry> {

	private ProviderStrategryType stategry;
	
	private static Map<ProviderStrategryType, ProviderStrategy<ServiceEntry>> stategrys = new HashMap<>();
	
	static {
		stategrys.put(ProviderStrategryType.RANDOM, new RandomStrategy<ServiceEntry>());
		stategrys.put(ProviderStrategryType.ROUNDROBIN, new RoundRobinStrategy<ServiceEntry>());
		stategrys.put(ProviderStrategryType.INTSTICKY, new IntStickyStrategy<ServiceEntry>());
	}
	
	public ServiceRoute() {
		this.stategry = ProviderStrategryType.ROUNDROBIN;
	}
	
	public void setProviderStrategryType(ProviderStrategryType stategry) {
		this.stategry = stategry;
	}
	
	public void setProviderStrategryType(ProviderStrategryType stategry, int id) {
		this.stategry = stategry;
		
		if(stategry == ProviderStrategryType.INTSTICKY) {
			
			((IntStickyStrategy<ServiceEntry>)stategrys.get(stategry)).setId(id);
		}
	}

	@Override
	public ServiceInstance<ServiceEntry> getInstance(InstanceProvider<ServiceEntry> instanceProvider) throws Exception {
		
		return stategrys.get(stategry).getInstance(instanceProvider);
	}

}
