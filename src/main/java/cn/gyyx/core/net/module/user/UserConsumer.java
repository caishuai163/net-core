package cn.gyyx.core.net.module.user;

import cn.gyyx.core.net.queue.QueueServerConsumer;

public class UserConsumer extends QueueServerConsumer {

	public UserConsumer(UserService userService) throws Exception {
		super(userService);
	}
}
