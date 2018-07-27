package cn.gyyx.core.net.module;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.x.discovery.ServiceInstance;

import java.util.Map;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.codec.ResultInfo;
import cn.gyyx.core.net.codec.StatusCode;
import cn.gyyx.core.net.mgr.ClientSessionMgr;
import cn.gyyx.core.net.mgr.ConnectMgr;
import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.protocol.ProtoType;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ack;
import cn.gyyx.core.net.protocol.p_module_common.p_module_common_ping;
import cn.gyyx.core.net.queue.ClientEventInfo;
import cn.gyyx.core.net.queue.ClientNonLockQueue;
import cn.gyyx.core.net.service.ProviderStrategryType;
import cn.gyyx.core.net.service.ServiceDiscover;
import cn.gyyx.core.net.service.ServiceEntry;
import io.netty.channel.Channel;


public abstract class ModuleClientService {

	protected ProtoHandlerMgr protohandlerMgr;
	
	protected ConnectMgr connectMgr;
	
	private ClientSessionMgr clientSessionMgr;
	
	private ServiceDiscover discover; 
	
	private static final int DEFALT_TIMEOUT = 30;
	
	private Map<Long, SyncContext> syncContexts = new ConcurrentHashMap<>();
	
	private AtomicLong syncGuid = new AtomicLong(0);

	public ModuleClientService(ProtoHandlerMgr protohandlerMgr, ConnectMgr connectMgr, ServiceDiscover discover) {
		this.protohandlerMgr = protohandlerMgr;
		this.connectMgr = connectMgr;
		this.clientSessionMgr = connectMgr.getClientSessionMgr();
		this.discover = discover;
	}
	
	public SyncContext getSyncContext(long syncId) {
		return this.syncContexts.get(syncId);
	}
	
	public ResultInfo sendSyncMsg(String serviceName, GeneratedMessage proto, ProviderStrategryType strategryType) throws InterruptedException {

		long id = syncGuid.incrementAndGet();
		
		SyncContext context = new SyncContext();
		
		CountDownLatch latch = new CountDownLatch(1);

		context.setLatch(latch);
		context.setSyncId(id);
		
		ClientEventInfo eventInfo = new ClientEventInfo();
		
		eventInfo.setId(id);
		eventInfo.setServiceName(serviceName);
		eventInfo.setBody(proto);
		eventInfo.setStrategryType(strategryType);
		
		syncContexts.put(id, context);
		
		ClientNonLockQueue.publish(eventInfo);
		
		try {
			latch.await();

			return context.getResult();
		} finally {
			syncContexts.remove(id);
		}
	}
	
	
	
	public ResultInfo onClientSend(ClientEventInfo data, ServiceEntry entry) throws InterruptedException {

		CountDownLatch latch = new CountDownLatch(1);
	
		SyncContext context = new SyncContext();
		context.setSyncId(data.getId());
		context.setLatch(latch);
		
		Channel channel = connectMgr.connect(data.getServiceName(), entry.getIp(), entry.getPort());
		
		if(channel == null) {
			ResultInfo result = new ResultInfo();
			result.setErrorCode(StatusCode.CONNECTEXCEPTION);
			return result;
		}
		
		clientSessionMgr.updateSyncContext(context, channel);
		clientSessionMgr.sendMsg(channel, data.getBody());

		if(!latch.await(DEFALT_TIMEOUT, TimeUnit.SECONDS)) {
			ResultInfo result = new ResultInfo();
			result.setErrorCode(StatusCode.TIMEOUT);
			return result;
		}

		return context.getResult();
		
	}
	
	public ServiceEntry getServiceAddress(String serviceName, ProviderStrategryType strategryType) {
		try {
			ServiceInstance<ServiceEntry> instance = discover.getService(serviceName, strategryType);
			
			if(instance == null) {
				return null;
			}
			
			return instance.getPayload();
			
		} catch(Throwable ex) {
			ex.printStackTrace();
			return null;
		}
		
	}
	
	public void init() {
		
		registerProto();
	}
	
	protected void registerProto() {
		protohandlerMgr.registerProto(ProtoType.P_MODULE_COMMON_PING, p_module_common_ping.class);
		protohandlerMgr.registerProto(ProtoType.P_MODULE_COMMON_ACK, p_module_common_ack.class);
		registerProtoImpl();
	}
	
	protected abstract void registerProtoImpl();
}
