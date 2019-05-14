package io.inprice.scrapper.manager.helpers;

import io.inprice.scrapper.common.config.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPools {

	public static final ExecutorService MASTER_POOL;

	static {
		MASTER_POOL = new ThreadPoolExecutor(
			Config.TPOOLS_MASTER_CAPACITY,
			Config.TPOOLS_MASTER_CAPACITY,
			0L,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>()
		);
	}

}
