package io.inprice.manager.scheduled.publisher;

import java.util.List;

import io.inprice.common.models.Link;
import io.inprice.manager.dao.LinkDao;

public class NewlyAddedLinksPublisher extends AbstractLinkPublisher {

	@Override
	String getTaskName() {
		return "ADDED-LINK-PUBLISHER";
	}

	@Override
	List<Link> findLinks(LinkDao linkDao) {
		return linkDao.findNewlyAddedLinks();
	}

}
