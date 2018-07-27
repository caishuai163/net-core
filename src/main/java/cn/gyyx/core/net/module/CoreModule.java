package cn.gyyx.core.net.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public abstract class CoreModule extends AbstractModule {

	@Override
	protected void configure() {
		
		binder().requireExplicitBindings();
		
		configChildModule();
        configCoreModule();
	}
	
	protected abstract void configChildModule();
	
	private void configCoreModule() {
	
		bind(ModuleServerService.class).in(Singleton.class);
	}

}
