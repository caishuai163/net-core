package cn.gyyx.core.net.queue;

import org.apache.curator.x.discovery.ServiceProvider;

import com.lmax.disruptor.EventHandler;

import cn.gyyx.core.net.codec.ResultInfo;
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
     * <li></li>
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

        SyncContext userContext = moduleService.getSyncContext(event.getId());

        if (userContext != null) {
            userContext.setResult(result);
        }
    }

}
