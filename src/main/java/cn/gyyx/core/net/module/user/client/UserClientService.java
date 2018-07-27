package cn.gyyx.core.net.module.user.client;

import cn.gyyx.core.net.codec.ResultInfo;
import cn.gyyx.core.net.codec.StatusCode;
import cn.gyyx.core.net.mgr.ConnectMgr;
import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.module.ModuleClientService;
import cn.gyyx.core.net.protocol.p_module_user.p_module_user_login_request;
import cn.gyyx.core.net.protocol.p_module_user.p_module_user_login_result;
import cn.gyyx.core.net.service.ProviderStrategryType;
import cn.gyyx.core.net.service.ServiceDiscover;


public class UserClientService extends ModuleClientService {

	public UserClientService(ProtoHandlerMgr protohandlerMgr, ConnectMgr connectMgr, ServiceDiscover discover) throws Exception {
		super(protohandlerMgr, connectMgr, discover);
		
	}
	
	public p_module_user_login_result accountLogin(String account, String password) throws InterruptedException {
		p_module_user_login_request.Builder builder = p_module_user_login_request.newBuilder();

		builder.setAccount(account);
		builder.setPassword(password);
		
		ResultInfo resultInfo = this.sendSyncMsg("UserService", builder.build(), ProviderStrategryType.ROUNDROBIN);
		
		if(resultInfo == null || resultInfo.getErrorCode() == StatusCode.CONNECTIONCLOSED) {
			return null;
		}
		
		return (p_module_user_login_result)resultInfo.getData();
	}

	@Override
	protected void registerProtoImpl() {
		
		protohandlerMgr.registerProto(p_module_user_login_request.class);
		protohandlerMgr.registerProto(p_module_user_login_result.class);
		
	}
}
