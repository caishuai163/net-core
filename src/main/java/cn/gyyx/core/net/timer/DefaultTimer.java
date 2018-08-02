package cn.gyyx.core.net.timer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 定时器，执行定时任务
 */
public class DefaultTimer {

    private Timer timer;

    private int interval = 10000;

    /**
     * 默认定时器,仅指定定时执行的方法,默认10秒执行一次
     * 
     * @param callback
     *            定时执行的方法
     */
    public DefaultTimer(TimerCallBack callback) {
        timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callback.callback();
            }
        }, 0, interval);
    }

    /**
     * 自定义定时执行时间的定时器
     * 
     * @param callback
     *            定时执行的方法
     * @param interval
     *            多久执行一次 单位:毫秒
     */
    public DefaultTimer(TimerCallBack callback, int interval) {
        timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callback.callback();
            }
        }, 0, interval);
    }

    /**
     * 定时器关闭
     */
    public void close() {
        timer.cancel();
    }
}
