package io.inprice.manager.scheduled.publisher;

import io.inprice.common.lib.ExpiringHashSet;
import io.inprice.common.lib.ExpiringSet;
import io.inprice.manager.config.Props;

/**
 * A kind of helper class for all link publishers to avoid sending the same url successively
 * 
 * @author mdpinar
 * @since 2021-08-31
 */
public class PublishedLinkChecker {

	private static final ExpiringSet<String> expiringSet;
	
	static {
		expiringSet = new ExpiringHashSet<>(Props.getConfig().APP.LINK_REVIEW_PERIOD * 59 * 1000);
	}

	static boolean hasAlreadyPublished(String hash) {
		return expiringSet.contains(hash);
	}

	static boolean published(String hash) {
		return expiringSet.add(hash);
	}

}
