package cn.gyyx.core.net.service;

import java.util.List;

import org.apache.curator.x.discovery.ProviderStrategy;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceProvider;

/**
 * 固定路由的路由策略模式的实现
 * 
 * @param <T>
 */
public class IntStickyStrategy<T> implements ProviderStrategy<T> {

    private int id = 0;

    public void setId(int id) {
        this.id = id;
    }

    /**
     * 固定路由模式获取服务
     * 
     * @param instanceProvider
     */
    @Override
    public ServiceInstance<T> getInstance(InstanceProvider<T> instanceProvider)
            throws Exception {
        /**
         * provider 获取所有的服务
         */
        List<ServiceInstance<T>> instances = instanceProvider.getInstances();

        if (instances.size() == 0) {
            return null;
        }

        int thisIndex = id;
        /**
         * 获取第数据取余数个，防止出现固定路由Id超出路由总数的情况
         */
        return instances.get(thisIndex % instances.size());
    }
}
