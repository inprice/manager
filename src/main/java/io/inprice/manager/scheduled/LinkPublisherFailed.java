package io.inprice.manager.scheduled;

import java.util.List;

import io.inprice.common.models.Link;
import io.inprice.manager.dao.LinkDao;

public class LinkPublisherFailed extends LinkPublisherAbstract {

	private int retry;
	private int interval;
	private String timeUnit;

	public LinkPublisherFailed(int retry, int interval, String timeUnit) {
		super();
		this.retry = retry;
		this.interval = interval;
		this.timeUnit = timeUnit;
	}

	@Override
	String getTaskName() {
		return "FAILED-LINK-PUBLISHER ["+retry+"]";
	}

	@Override
	List<Link> findLinks(LinkDao linkDao) {
		return linkDao.findFailedLinks(retry, interval, timeUnit);
	}

}
