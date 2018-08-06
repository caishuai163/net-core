package cn.gyyx.core.net.mgr;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * zookeeper连接管理
 */
public class CuratorMgr {

    private CuratorFramework client;

    private static final int sleepMs = 1000;

    private static final int retryCount = 3;
    /** 默认的 zookeeper 连接串 */
//    private static final String DefaultConnectionString = "10.14.28.43:2181";
    private static final String DefaultConnectionString = "127.0.0.1:2181";

    /**
     * 使用默认的zookeeper连接串{@link #DefaultConnectionString}进行连接
     * 
     * @see #CuratorMgr(String)
     */
    public CuratorMgr() {
        this(DefaultConnectionString);
    }

    /**
     * <h3>使用指定的zookeeper连接串进行连接</h3>创建zookeeper连接客户端并启动
     * 
     * @param connectString
     *            指定的连接串 <code>ip:port</code>
     */
    public CuratorMgr(String connectString) {
        /** 创建zookeeper连接客户端并启动 */
        client = CuratorFrameworkFactory.newClient(connectString,
            new ExponentialBackoffRetry(sleepMs, retryCount));
        client.start();
    }

    /**
     * 获取zookeeper连接客户端
     *
     * @return {@link CuratorFramework}
     */
    public CuratorFramework getCuratorClient() {
        return client;
    }

    /**
     * 关闭zookeeper连接客户端
     */
    public void close() {
        client.close();
    }
}
