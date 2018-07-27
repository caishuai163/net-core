package cn.gyyx.core.net.module.user.client;

import cn.gyyx.core.net.queue.QueueClientConsumer;

public class UserClientConsumer extends QueueClientConsumer  {

	public UserClientConsumer(UserClientService userClientService) throws Exception {
		
		super(userClientService);

	}
}
