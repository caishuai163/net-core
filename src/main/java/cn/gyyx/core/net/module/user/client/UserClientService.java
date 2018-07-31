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

    public UserClientService(ProtoHandlerMgr protohandlerMgr,
            ConnectMgr connectMgr, ServiceDiscover discover) throws Exception {
        super(protohandlerMgr, connectMgr, discover);

    }

    /**
     * 客户端登陆请求
     *
     * @param account
     * @param password
     * @return
     * @throws InterruptedException
     *             p_module_user_login_result
     */
    public p_module_user_login_result accountLogin(String account,
            String password) throws InterruptedException {
        /** 构造一个用户登陆请求的prortBuf构造器builder */
        p_module_user_login_request.Builder builder = p_module_user_login_request
                .newBuilder();
        /** 放入登陆请求的数据 */
        builder.setAccount(account);
        builder.setPassword(password);
        /** 发起登陆请求 */
        ResultInfo resultInfo = this.sendSyncMsg("UserService", builder.build(),
            ProviderStrategryType.ROUNDROBIN);
        /** 如果返回的结果不正常 */
        if (resultInfo == null
                || resultInfo.getErrorCode() == StatusCode.CONNECTIONCLOSED) {
            return null;
        }

        return (p_module_user_login_result) resultInfo.getData();
    }

    /**
     * 除了在父类里的心跳包数据，增加登陆与登陆返回数据在protobuf中处理的注册（init）
     */
    @Override
    protected void registerProtoImpl() {

        protohandlerMgr.registerProto(p_module_user_login_request.class);
        protohandlerMgr.registerProto(p_module_user_login_result.class);

    }
}
