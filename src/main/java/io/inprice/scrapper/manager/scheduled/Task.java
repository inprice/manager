package io.inprice.scrapper.manager.scheduled;

import io.inprice.scrapper.common.info.TimePeriod;

public interface Task extends Runnable {

    TimePeriod getTimePeriod();

}
