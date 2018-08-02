package cn.gyyx.core.net.module.user;

import cn.gyyx.core.net.mgr.ProtoHandlerMgr;
import cn.gyyx.core.net.mgr.TcpServerMgr;
import cn.gyyx.core.net.module.ModuleServerService;
import cn.gyyx.core.net.protocol.p_module_user.p_module_user_login_request;
import cn.gyyx.core.net.protocol.p_module_user.p_module_user_login_result;
import cn.gyyx.core.net.service.ServiceRegister;
import io.netty.channel.Channel;

/**
 * 实际上的服务器端业务实现类
 */
public class UserService extends ModuleServerService {

    private static final String serviceName = "UserService";

    /**
     * 实例化对象
     * 
     * @param protohandlerMgr
     * @param tcpServerMgr
     * @param serviceRegister
     * @throws Exception
     */
    public UserService(ProtoHandlerMgr protohandlerMgr,
            TcpServerMgr tcpServerMgr, ServiceRegister serviceRegister)
            throws Exception {

        super(protohandlerMgr, tcpServerMgr, serviceRegister);
    }

    /**
     * <h3>初始化注册</h3>
     * <ul>
     * <li>数据处理时遇到什么类型的数据用于什么方法(handler)解析</br>
     * --登陆请求数据类型{@link p_module_user_login_request}使用{@link #accountLoginHandler(long, Channel, Object)}处理</li>
     * <li>注册数据类型{@link p_module_user_login_result}</li>
     * </ul>
     */
    @Override
    protected void registerProtoHandlerImpl() {
        protohandlerMgr.registerHandler(p_module_user_login_request.class,
            this::accountLoginHandler);
        protohandlerMgr.registerProto(p_module_user_login_result.class);
    }

    @Override
    protected String getServiceName() {
        return serviceName;
    }

    /**
     * 接收登陆类数据，并进行处理，返回登陆结果
     * 
     * @param requestId
     * @param channel
     * @param proto
     * @return {@link p_module_user_login_result}
     */
    public p_module_user_login_result accountLoginHandler(long requestId,
            Channel channel, Object proto) {
        /**
         * 数据类型装换
         */
        p_module_user_login_request p = (p_module_user_login_request) proto;
        /**
         * 构造返回数据
         */
        p_module_user_login_result.Builder builder = p_module_user_login_result
                .newBuilder();
        /**
         * 抽奖业务
         */
        String lotteryResult = LotteryCache_pipline.lottery(p.getAccount(),
            p.getPassword());

        /**
         * 添加返回数据并返回结果
         */
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
