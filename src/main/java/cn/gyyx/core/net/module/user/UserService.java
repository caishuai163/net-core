package cn.gyyx.core.net.module.user;

import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.mgr.TcpServerMgr;
import cn.gyyx.core.net.module.ModuleServerService;
import cn.gyyx.core.net.protocol.p_module_user.p_module_user_login_request;
import cn.gyyx.core.net.protocol.p_module_user.p_module_user_login_result;
import cn.gyyx.core.net.service.ServiceRegister;
import io.netty.channel.Channel;

public class UserService extends ModuleServerService {

	private static final String serviceName = "UserService";
	
	public UserService(ProtoHandlerMgr protohandlerMgr, TcpServerMgr tcpServerMgr, ServiceRegister serviceRegister) throws Exception {
		
		super(protohandlerMgr, tcpServerMgr, serviceRegister);
	}
	
	@Override
	protected void registerProtoHandlerImpl() {
		protohandlerMgr.registerHandler(p_module_user_login_request.class, this::accountLoginHandler);
		protohandlerMgr.registerProto(p_module_user_login_result.class);
	}
	
	@Override
	protected String getServiceName() {
		return serviceName;
	}
	
	public p_module_user_login_result accountLoginHandler(long requestId, Channel channel ,Object proto) {
		p_module_user_login_request p = (p_module_user_login_request) proto;
		
		p_module_user_login_result.Builder builder = p_module_user_login_result.newBuilder();
		
		String lotteryResult = LotteryCache_pipline.lottery(p.getAccount(), p.getPassword());

        if (lotteryResult.contains("error")) {
            builder.setIsSuccess(false);
            builder.setMessage("失败");
        } else {
            builder.setIsSuccess(true);
            builder.setMessage("成功");
        }

		return builder.build();
	}
	
}
