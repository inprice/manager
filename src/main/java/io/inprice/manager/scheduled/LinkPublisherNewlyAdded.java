package io.inprice.manager.scheduled;

import java.util.List;

import io.inprice.common.models.Link;
import io.inprice.manager.dao.LinkDao;

public class LinkPublisherNewlyAdded extends LinkPublisherAbstract {

	@Override
	String getTaskName() {
		return "NEWLY-ADDED-LINK-PUBLISHER";
	}

	@Override
	List<Link> findLinks(LinkDao linkDao) {
		return linkDao.findNewlyAddedLinks();
	}

}
