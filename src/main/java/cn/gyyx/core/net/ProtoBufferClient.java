package cn.gyyx.core.net;

import org.apache.thrift.transport.TTransportException;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.gyyx.core.net.protocol.p_module_user;
import cn.gyyx.core.net.protocol.p_module_user.p_module_user_login_request;

/**
 * Hello world!
 *
 */
public class ProtoBufferClient {
    public static void main(String[] args)
            throws InvalidProtocolBufferException, TTransportException {
        p_module_user.p_module_user_login_request request = p_module_user.p_module_user_login_request
                .newBuilder().setAccount("aaa").setPassword("123456").build();

        System.out.println(request.toByteString());

        System.out.println(p_module_user_login_request.PARSER);

        p_module_user.p_module_user_login_request request2 = p_module_user.p_module_user_login_request
                .parseFrom(request.toByteArray());

        System.out.println(request2.toString());

    }
}
