package io.inprice.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Channel;

import io.inprice.common.models.Link;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.LinkDao;

public class NewlyAddedLinksPublisher extends AbstractLinkPublisher {

	public NewlyAddedLinksPublisher(Channel scrappingLinksChannel, Channel statusChangingLinksChannel) {
		super(Props.getConfig().SCHEDULES.NEWLY_ADDED_LINK_PUBLISHER, scrappingLinksChannel, statusChangingLinksChannel);
	}

	@Override
	String getTaskName() {
		return "NewlyAddedLinksPublisher";
	}

	@Override
	List<Link> findLinks(LinkDao linkDao) {
		return linkDao.findNewlyAddedLinks();
	}

}
