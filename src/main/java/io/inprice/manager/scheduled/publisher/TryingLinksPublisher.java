package io.inprice.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Channel;

import io.inprice.common.config.SchedulerDef;
import io.inprice.common.meta.Grup;
import io.inprice.common.models.Link;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.LinkDao;

public class TryingLinksPublisher extends AbstractLinkPublisher {

	private int retry;

	public TryingLinksPublisher(SchedulerDef scheduler, Channel scrappingLinksChannel, Channel statusChangingLinksChannel) {
		super(scheduler, scrappingLinksChannel, statusChangingLinksChannel);
		this.retry = Integer.valueOf(scheduler.DATA.get("retry").toString());
	}

	@Override
	String getTaskName() {
		return getClass().getSimpleName() + ":R-"+retry;
	}

	@Override
	List<Link> findLinks(LinkDao linkDao) {
		return linkDao.findActiveOrTryingLinks(
			Grup.TRYING, 
			retry, 
			Props.getConfig().LIMITS.LINK_LIMIT_FETCHING_FROM_DB,
			Props.getConfig().APP.LINK_REVIEW_PERIOD
		);
	}

}
