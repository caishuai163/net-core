package cn.gyyx.core.net.service;

/**
 * 所提供的策略类型
 */
public enum ProviderStrategryType {
    /**
     * 随机
     */
    RANDOM,
    /**
     * 轮询调度
     */
    ROUNDROBIN,
    /**
     * 固定路由
     */
    INTSTICKY
}
