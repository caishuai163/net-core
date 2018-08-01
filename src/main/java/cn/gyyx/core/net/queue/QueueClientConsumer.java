package cn.gyyx.core.net.queue;

import org.apache.curator.x.discovery.ServiceProvider;

import com.lmax.disruptor.EventHandler;

import cn.gyyx.core.net.codec.ClientEncoder;
import cn.gyyx.core.net.codec.ResultInfo;
import cn.gyyx.core.net.codec.ServerDecoder;
import cn.gyyx.core.net.codec.StatusCode;
import cn.gyyx.core.net.mgr.ServiceDiscoverMgr;
import cn.gyyx.core.net.module.ModuleClientService;
import cn.gyyx.core.net.module.SyncContext;
import cn.gyyx.core.net.service.ProviderStrategryType;
import cn.gyyx.core.net.service.ServiceDiscover;
import cn.gyyx.core.net.service.ServiceEntry;

/**
 * <p>
 * 客户端事件处理（消费队列）
 * </p>
 */
public class QueueClientConsumer implements EventHandler<ClientEventInfo> {
    /**
     * 定义客户端消费队列中的业务处理service
     */
    protected ModuleClientService moduleService;

    /**
     * 构造方法。</br>
     * 在初始化对象时，传入{@link ModuleClientService}(客户端消费队列中的业务处理service),初始化ModuleClientService
     * 
     * @param moduleService
     *            客户端消费队列中的业务处理service
     */
    public QueueClientConsumer(ModuleClientService moduleService) {
        this.moduleService = moduleService;
    }

    /**
     * 初始化客户端消费事件</br>
     * 调用{@link ModuleClientService#init()}方法
     *
     * @throws Exception
     *             void
     */
    protected void init() throws Exception {
        moduleService.init();
    }

    /**
     * <h3>消费队列执行消费者操作时，执行的操作。</h3>
     * <ul>
     * <li>获取消费者消费时需要发送的服务对象实例详情{@link ServiceEntry}(zookeeper上的服务),详情见{@link ModuleClientService#getServiceAddress(String, ProviderStrategryType)}</br>
     * <ul>
     * <li>{@link ServiceEntry}:服务实例详细bean(包括业务名，ip，端口)，</br>
     * 由服务实例{@link ServiceInstance#getPayload()}得到</li>
     * <li>{@link ServiceInstance}:服务实例，</br>
     * 需要从已经缓存的服务provider({@link ServiceProvider})或者通过服务注册&发现Mgr(manager{@link ServiceDiscoverMgr#get()})去zookeeper中查找服务,</br>
     * 并设定其路由模式。</br>
     * more info see
     * {@link ServiceDiscover#getService(String, ProviderStrategryType)}</li>
     * </ul>
     * </li>
     * <li>调用数据发送方法{@link ModuleClientService#onClientSend(ClientEventInfo, ServiceEntry)}，并获得返回值
     * <ul>
     * <li>首先是创建一个netty的channel</li>
     * <li>创建channel后，向channel中写入数据.</br>
     * 实际上，channel在初始化的时候，已经初始化好了decode和encode，插入到channel执行的pipeline里
     * <ul>
     * <li>客户端发送数据编码{@link ClientEncoder}</li>
     * <li>客户端返回数据解码{@link ServerDecoder}</li>
     * </ul>
     * 在客户端解码过程中，会去异步调用查找请求时channel对应的session的上下文信息，将返回值放到上下文中，并释放上下文(发送数据的整个方法)中的锁，返回数据{@link ModuleClientService#onClientSend(ClientEventInfo, ServiceEntry)}
     * </li>
     * </ul>
     * </li>
     * <li>执行网络请求后，将返回的数据放到生产者上下文所对应的响应结果中(异步调用)</br>
     * <ul>
     * 执行结束，获取事件id对应的同步文本信息</br>
     * 这里实际上是在获取生产者放入队列外面嵌套的程序块的信息</br>
     * 之前我们在将event放入队列之前，上了一个锁，并将锁存入了该次过程的上下文信息中，</br>
     * 这里获取到当时执行的上下文</br>
     * 获取到上下文后，将消费者在请求服务器端返回的数据拍到上下文中，拍的过程中，会将上下文{@link ModuleClientService#sendSyncMsg(String, com.google.protobuf.GeneratedMessage, ProviderStrategryType)}中设置的锁给释放掉，上下文对应的代码块就可以继续执行下去了
     * </ul>
     * </li>
     * </ul>
     * 
     * @see EventHandler#onEvent(Object, long, boolean)
     */
    @Override
    public void onEvent(ClientEventInfo event, long sequence,
            boolean endOfBatch) throws Exception {

        ResultInfo result = null;
        ServiceEntry firstEntry = null;
        long retryCount = 1;

        for (;;) {
            try {
                /**
                 * 获取消费者消费时需要发送的服务对象实例详情，zookeeper上的服务</br>
                 * <ul>
                 * <li>ServiceEntry:服务实例详细bean(包括业务名，ip，端口)，</br>
                 * 由服务实例ServiceInstance.getPayload()得到</li>
                 * <li>ServiceInstance:服务实例，</br>
                 * 需要从已经缓存的服务provider或者通过服务注册&发现Mgr(manager)去zookeeper中查找服务,</br>
                 * 并设定其路由模式</li>
                 * </ul>
                 */
                ServiceEntry entry = moduleService.getServiceAddress(
                    event.getServiceName(), event.getStrategryType());

                if (entry == null) {
                    continue;
                }
                /**
                 * 如果是固定路由模式，仅使用第一次获取到的服务详情信息
                 */
                if (event
                        .getStrategryType() == ProviderStrategryType.INTSTICKY) {
                    if (retryCount == 1) {
                        firstEntry = entry;
                    } else if (!entry.equals(firstEntry)) {
                        break;
                    }
                }
                /**
                 * 仅执行一次操作
                 */
                if (retryCount > 1) {
                    break;
                }
                /** 客户端发送事件业务流程 */
                result = moduleService.onClientSend(event, entry);

                if (result.getErrorCode() > 0 || result
                        .getErrorCode() == StatusCode.CONNECTIONCLOSED) {
                    break;
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                result = new ResultInfo();
                result.setErrorCode(StatusCode.EXCEPTION);
            } finally {
                retryCount = retryCount + 1;
            }
        }
        /**
         * 执行结束，获取事件id对应的同步文本信息</br>
         * 这里实际上是在获取生产者放入队列外面嵌套的程序块的信息</br>
         * 之前我们在将event放入队列之前，上了一个锁，并将锁存入了该次过程的上下文信息中，</br>
         * 这里获取到当时执行的上下文
         */
        SyncContext userContext = moduleService.getSyncContext(event.getId());
        /**
         * 获取到上下文后，将消费者在请求服务器端返回的数据拍到上下文中，拍的过程中，会将上下文中设置的锁给释放掉，上下文对应的代码块就可以继续执行下去了
         */
        if (userContext != null) {
            userContext.setResult(result);
        }
    }

}
