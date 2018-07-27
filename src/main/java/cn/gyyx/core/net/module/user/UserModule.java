package cn.gyyx.core.net.module.user;

import com.google.inject.Singleton;

import cn.gyyx.core.net.module.CoreModule;



public class UserModule extends CoreModule {

	@Override
	protected void configChildModule() {
		bind(UserService.class).in(Singleton.class);
		bind(UserConsumer.class).in(Singleton.class);
	}

}
