package io.inprice.manager.scheduled;

import io.inprice.common.info.TimePeriod;

public interface Task extends Runnable {

    TimePeriod getTimePeriod();

}
