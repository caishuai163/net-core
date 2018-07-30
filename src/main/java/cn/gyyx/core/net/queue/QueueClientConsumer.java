package cn.gyyx.core.net.queue;

import com.lmax.disruptor.EventHandler;

import cn.gyyx.core.net.codec.ResultInfo;
import cn.gyyx.core.net.codec.StatusCode;
import cn.gyyx.core.net.module.ModuleClientService;
import cn.gyyx.core.net.module.SyncContext;
import cn.gyyx.core.net.service.ProviderStrategryType;
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
     * 消费队列执行消费者操作时，执行的操作。
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
                ServiceEntry entry = moduleService.getServiceAddress(
                    event.getServiceName(), event.getStrategryType());

                if (entry == null) {
                    continue;
                }

                if (event
                        .getStrategryType() == ProviderStrategryType.INTSTICKY) {
                    if (retryCount == 1) {
                        firstEntry = entry;
                    } else if (!entry.equals(firstEntry)) {
                        break;
                    }
                }

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
