package cn.gyyx.core.net.timer;

import java.util.Timer;
import java.util.TimerTask;

public class DefaultTimer {

	private Timer timer;
	
	private int interval = 10000;
	
	public DefaultTimer(TimerCallBack callback) {
		timer = new Timer();
		
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				callback.callback();
			}
		}, 0,interval);
	}
	
	public DefaultTimer(TimerCallBack callback, int interval) {
		timer = new Timer();
		
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				callback.callback();
			}
		}, 0,interval);
	}
	
	public void close() {
		timer.cancel();
	}
}
