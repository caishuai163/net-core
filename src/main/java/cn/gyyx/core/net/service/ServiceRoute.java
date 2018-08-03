package cn.gyyx.core.net.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.curator.x.discovery.ProviderStrategy;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceProvider;
import org.apache.curator.x.discovery.details.ServiceProviderImpl;
import org.apache.curator.x.discovery.strategies.RandomStrategy;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;

/**
 * <h3>路由服务，管理zookeeper服务的路由策略类型</h3></br>
 * 由于继承了{@link ProviderStrategy},所以在<code>provider.getInstance()</code>时,调用的{@link ServiceProviderImpl#getInstance()}方法内的{@link ProviderStrategy}接口所对应的实现类实际上是本类，在{@link ProviderStrategy#getInstance(InstanceProvider)}时，调用的是本类里的{@link #getInstance(InstanceProvider)},返回Instance时指定了策略模式
 */
public class ServiceRoute implements ProviderStrategy<ServiceEntry> {

    private ProviderStrategryType stategry;

    private static Map<ProviderStrategryType, ProviderStrategy<ServiceEntry>> stategrys = new HashMap<>();
    /** 将所有类型缓存到Map中 方便在 getInstance()时快速定位返回 */
    static {
        stategrys.put(ProviderStrategryType.RANDOM,
            new RandomStrategy<ServiceEntry>());
        stategrys.put(ProviderStrategryType.ROUNDROBIN,
            new RoundRobinStrategy<ServiceEntry>());
        stategrys.put(ProviderStrategryType.INTSTICKY,
            new IntStickyStrategy<ServiceEntry>());
    }

    /**
     * new 对象 ，默认为轮询模式
     */
    public ServiceRoute() {
        this.stategry = ProviderStrategryType.ROUNDROBIN;
    }

    /**
     * 设置路由策略模式
     * 
     * @param stategry
     *            {@link ProviderStrategryType}
     */
    public void setProviderStrategryType(ProviderStrategryType stategry) {
        this.stategry = stategry;
    }

    /**
     * 设置路由策略模式，指定路由ID
     * 
     * @param stategry
     *            {@link ProviderStrategryType}
     * @param id
     *            固定路由模式下指定的路由id
     */
    public void setProviderStrategryType(ProviderStrategryType stategry,
            int id) {
        this.stategry = stategry;

        if (stategry == ProviderStrategryType.INTSTICKY) {
            /** 指定路由模式时，指定指定路由ID */
            ((IntStickyStrategy<ServiceEntry>) stategrys.get(stategry))
                    .setId(id);
        }
    }

    /**
     * <h3>获取当前使用中的路由策略</h3>
     * 
     */
    @Override
    public ServiceInstance<ServiceEntry> getInstance(
            InstanceProvider<ServiceEntry> instanceProvider) throws Exception {
        return stategrys.get(stategry).getInstance(instanceProvider);
    }

}
