package io.inprice.scrapper.manager.helpers;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.manager.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadPools {

	private static final Logger log = new Logger(ThreadPools.class);

	public static final ExecutorService PRICE_CHANGE_POOL;
	public static final ExecutorService DELETED_LINKS_POOL;
	public static final ExecutorService STATUS_CHANGE_POOL;
	public static final ExecutorService AVAILABLE_LINKS_POOL;

	private static final List<ExecutorService> registry;

	static {
		STATUS_CHANGE_POOL = Executors.newFixedThreadPool(2);
		AVAILABLE_LINKS_POOL = Executors.newFixedThreadPool(2);
		PRICE_CHANGE_POOL = Executors.newFixedThreadPool(1);
		DELETED_LINKS_POOL = Executors.newFixedThreadPool(1);

		registry = new ArrayList<>();
		registry.add(STATUS_CHANGE_POOL);
		registry.add(AVAILABLE_LINKS_POOL);
		registry.add(PRICE_CHANGE_POOL);
		registry.add(DELETED_LINKS_POOL);
	}

	public static void shutdown() {
		for (ExecutorService pool: registry) {
			try {
				pool.shutdown();
				pool.awaitTermination(Config.WAITING_TIME_FOR_AWAIT_TERMINATION, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				log.error("Thread pool termination is interrupted.", e);
			}
		}
	}

}
