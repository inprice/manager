package io.inprice.scrapper.manager.helpers;

import io.inprice.scrapper.common.config.Config;

import java.util.concurrent.*;

public class ThreadPools {

	public static final ExecutorService MASTER_POOL;

	public static final ExecutorService PRICE_CHANGE_POOL;
	public static final ExecutorService STATUS_CHANGE_POOL;

	static {
		MASTER_POOL = new ThreadPoolExecutor(
			Config.TPOOLS_MASTER_CAPACITY,
			Config.TPOOLS_MASTER_CAPACITY,
			0L,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>()
		);

		PRICE_CHANGE_POOL = Executors.newFixedThreadPool(1);
		STATUS_CHANGE_POOL = Executors.newFixedThreadPool(1);
	}

}
