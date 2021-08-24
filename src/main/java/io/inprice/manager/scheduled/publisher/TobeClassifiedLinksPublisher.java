package io.inprice.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Channel;

import io.inprice.common.config.ScheduleDef;
import io.inprice.common.models.Link;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.LinkDao;

public class TobeClassifiedLinksPublisher extends AbstractLinkPublisher {

	private int retry;
	private int interval;
	private String period;

	public TobeClassifiedLinksPublisher(ScheduleDef scheduler, Channel scrappingLinksChannel, Channel statusChangingLinksChannel) {
		super(scheduler, scrappingLinksChannel, statusChangingLinksChannel);
		this.retry = Integer.valueOf(scheduler.DATA.get("retry").toString());
		this.interval = scheduler.EVERY;
		this.period = scheduler.PERIOD.substring(0, scheduler.PERIOD.length()-1);
	}

	@Override
	String getTaskName() {
		return getClass().getName() + ":R-"+retry;
	}

	@Override
	List<Link> findLinks(LinkDao linkDao) {
		return linkDao.findTobeClassifiedLinks(retry, interval, period, Props.getConfig().LIMITS.LINK_LIMIT_FETCHING_FROM_DB);
	}

}
