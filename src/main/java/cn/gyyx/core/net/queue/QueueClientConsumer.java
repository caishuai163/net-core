package cn.gyyx.core.net.queue;

import com.lmax.disruptor.EventHandler;

import cn.gyyx.core.net.codec.ResultInfo;
import cn.gyyx.core.net.codec.StatusCode;
import cn.gyyx.core.net.module.ModuleClientService;
import cn.gyyx.core.net.module.SyncContext;
import cn.gyyx.core.net.service.ProviderStrategryType;
import cn.gyyx.core.net.service.ServiceEntry;

public class QueueClientConsumer implements EventHandler<ClientEventInfo> {

	protected ModuleClientService moduleService;
	
	public QueueClientConsumer(ModuleClientService moduleService) {

		this.moduleService = moduleService;
	}
	
	protected void init() throws Exception {
		
		moduleService.init();
	}
	
	@Override
	public void onEvent(ClientEventInfo event, long sequence, boolean endOfBatch) throws Exception {
		
		ResultInfo result = null;
		ServiceEntry firstEntry = null;
		long retryCount = 1;
		
		for(;;) {
			try {
				 ServiceEntry entry = moduleService.getServiceAddress(event.getServiceName(), event.getStrategryType());
				 
				 if(entry == null) {
					 continue;
				 }
				 
				 if(event.getStrategryType() == ProviderStrategryType.INTSTICKY) {
					 if(retryCount == 1) {
						 firstEntry = entry;
					 } else if(!entry.equals(firstEntry)) {
						break;
					 }
				 }
				 
				 if(retryCount > 1) {
					 break;
				 }
				
				 result = moduleService.onClientSend(event, entry);
				 
				 if(result.getErrorCode() > 0 || result.getErrorCode() == StatusCode.CONNECTIONCLOSED) {
					 break;
				 }
			} catch(Throwable ex) {
				ex.printStackTrace();
				result = new ResultInfo();
				result.setErrorCode(StatusCode.EXCEPTION);
			} finally {
				retryCount = retryCount + 1;
			}
		}
		
		SyncContext userContext = moduleService.getSyncContext(event.getId());
		
		if(userContext != null) {
			userContext.setResult(result);
		}
	}

}
