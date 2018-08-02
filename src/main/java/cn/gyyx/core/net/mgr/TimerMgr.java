package cn.gyyx.core.net.mgr;

import java.util.ArrayList;
import java.util.List;

import cn.gyyx.core.net.timer.DefaultTimer;

/**
 * 定时器管理
 */
public class TimerMgr {

    private List<DefaultTimer> timers;

    /**
     * 实例化一个定时器管理，一个定时器管理里面会有n个定时任务图Timer schedule
     */
    public TimerMgr() {
        timers = new ArrayList<DefaultTimer>();
    }

    /**
     * 管理器里增加一个Timer schedule
     * 
     * @param timer
     *            {@link DefaultTimer}
     */
    public void add(DefaultTimer timer) {
        timers.add(timer);
    }

    /**
     * 关闭所有的timer schedule
     */
    public void close() {
        for (DefaultTimer timer : timers) {
            timer.close();
        }
    }
}
