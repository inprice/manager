package io.inprice.manager.scheduled;

import io.inprice.common.config.ScheduleDef;

public interface Task extends Runnable {

	public ScheduleDef getSchedule();

}
