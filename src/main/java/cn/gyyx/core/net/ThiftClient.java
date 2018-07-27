package cn.gyyx.core.net;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import cn.gyyx.core.net.rpc.UserService;

public class ThiftClient {

	public static void main(String[] args) throws TException {
		
		System.out.println("客户端启动....");
		
		TTransport transport = null;
		
		try {
			transport = new TSocket("localhost",9000);
			TProtocol protocol = new TBinaryProtocol(transport);
			
			UserService.Client client = new UserService.Client(protocol);
			
			transport.open();
			String result = client.getUser("aa");
			
			
			System.out.println(result);
		} finally {
			if(transport != null) {
				transport.close();
			}
		}
		
		
	}
}
