package io.inprice.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Connection;

import io.inprice.common.config.ScheduleDef;
import io.inprice.common.models.Link;
import io.inprice.manager.dao.LinkDao;

public class FailedLinksPublisher extends AbstractLinkPublisher {

	private final int retry;
	private final int interval;
	private final String period;

	public FailedLinksPublisher(ScheduleDef scheduler, Connection mqConn) {
		super(scheduler, mqConn);
		this.retry = Integer.valueOf(scheduler.DATA.get("retry").toString());
		this.interval = scheduler.EVERY;
		this.period = scheduler.PERIOD.substring(0, scheduler.PERIOD.length()-1);
	}

	@Override
	String getTaskName() {
		return "FailedLinksPublisher:R"+retry;
	}

	@Override
	List<Link> findLinks(LinkDao linkDao) {
		return linkDao.findFailedLinks(retry, interval, period);
	}

}
