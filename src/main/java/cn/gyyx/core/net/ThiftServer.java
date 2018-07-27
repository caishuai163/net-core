package cn.gyyx.core.net;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

import cn.gyyx.core.net.rpc.UserService;
import cn.gyyx.core.net.rpc.UserServiceImpl;

public class ThiftServer {

	public static void main(String[] args) throws TTransportException {
		System.out.println("服务开始启动....");
		
		TProcessor processor = new UserService.Processor<UserService.Iface>(new UserServiceImpl());

		TServerSocket socket = new TServerSocket(9000);
		
		TServer.Args serverArgs = new TServer.Args(socket);
		serverArgs.processor(processor);
		serverArgs.protocolFactory(new TBinaryProtocol.Factory());
		
		TServer server = new TSimpleServer(serverArgs);
		
		server.serve();
		
		System.out.println("服务启动完成");
	}
}
