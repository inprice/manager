package io.inprice.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Connection;

import io.inprice.common.models.Link;
import io.inprice.manager.dao.LinkDao;

public class FailedLinksPublisher extends AbstractLinkPublisher {

	private final int retry;
	private final int interval;

	public FailedLinksPublisher(int retry, int interval, Connection mqConn) {
		super(mqConn);
		this.retry = retry;
		this.interval = interval;
	}

	@Override
	String getTaskName() {
		return "FAILED-LINK-PUBLISHER ["+retry+"]";
	}

	@Override
	List<Link> findLinks(LinkDao linkDao) {
		return linkDao.findFailedLinks(retry, interval);
	}

}
