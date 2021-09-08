package io.inprice.manager.scheduled;

import io.inprice.common.config.SchedulerDef;

public interface Task extends Runnable {

	public SchedulerDef getScheduler();

}
