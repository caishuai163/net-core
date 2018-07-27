package cn.gyyx.core.net.mgr;

import java.util.ArrayList;
import java.util.List;

import cn.gyyx.core.net.timer.DefaultTimer;

public class TimerMgr {

	private List<DefaultTimer> timers;
	
	public TimerMgr() {
		timers = new ArrayList<DefaultTimer>();
	}
	
	public void add(DefaultTimer timer) {
		timers.add(timer);
	}
	
	public void close() {
		for(DefaultTimer timer:timers) {
			timer.close();
		}
	}
}
