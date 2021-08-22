package io.inprice.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Channel;

import io.inprice.common.config.ScheduleDef;
import io.inprice.common.models.Link;
import io.inprice.manager.dao.LinkDao;

public class ActiveLinksPublisher extends AbstractLinkPublisher {

	private int retry;
	private int interval;
	private String period;

	public ActiveLinksPublisher(ScheduleDef scheduler, Channel chForScrapping, 
  		Channel chForStatusChanging, Channel chForPlatformChanging) {
		super(scheduler, chForScrapping, chForStatusChanging, chForPlatformChanging);
		this.retry = Integer.valueOf(scheduler.DATA.get("retry").toString());
		this.interval = scheduler.EVERY;
		this.period = scheduler.PERIOD.substring(0, scheduler.PERIOD.length()-1);
	}

	@Override
	String getTaskName() {
		return "ActiveLinksPublisher:R"+retry;
	}

	@Override
	List<Link> findLinks(LinkDao linkDao) {
		return linkDao.findActiveLinks(retry, interval, period);
	}

}
