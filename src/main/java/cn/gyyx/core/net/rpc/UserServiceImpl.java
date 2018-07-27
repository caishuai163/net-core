package cn.gyyx.core.net.rpc;

import org.apache.thrift.TException;

public class UserServiceImpl implements UserService.Iface {

	@Override
	public String getUser(String name) throws TException {
		return "hello" + name;
	}

}
