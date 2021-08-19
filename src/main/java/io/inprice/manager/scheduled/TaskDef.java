package io.inprice.manager.scheduled;

import io.inprice.common.info.TimePeriod;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskDef {

	private Runnable task;
	private int delay;
	private TimePeriod timePeriod;

}
