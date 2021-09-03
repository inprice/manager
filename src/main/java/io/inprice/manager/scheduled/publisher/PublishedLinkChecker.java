package io.inprice.manager.scheduled.publisher;

import io.inprice.common.lib.ExpiringHashSet;
import io.inprice.common.lib.ExpiringSet;

/**
 * A kind of helper class for all link publishers to avoid sending the same url successively
 * 
 * @author mdpinar
 * @since 2021-08-31
 */
public class PublishedLinkChecker {

	private static final ExpiringSet<String> expiringSet = new ExpiringHashSet<>(28 * 60 * 1000); //28 minutes

	static boolean hasAlreadyPublished(String hash) {
		return expiringSet.contains(hash);
	}

	static boolean published(String hash) {
		return expiringSet.add(hash);
	}

}
