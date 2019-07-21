package io.inprice.scrapper.manager.config;

public class Config {

	// RabbitMQ
	public static final String MQ_HOST;
	public static final int MQ_PORT;
	public static final String MQ_USERNAME;
	public static final String MQ_PASSWORD;

	// Exchange
	public static final String MQ_LINK_EXCHANGE;
	public static final String MQ_CHANGE_EXCHANGE;

	// Queues
	public static final String MQ_NEW_LINKS_QUEUE;
	public static final String MQ_FAILED_LINKS_QUEUE;
	public static final String MQ_AVAILABLE_LINKS_QUEUE;
	public static final String MQ_TOBE_AVAILABLE_LINKS_QUEUE;

	public static final String MQ_STATUS_CHANGE_QUEUE;
	public static final String MQ_PRICE_CHANGE_QUEUE;
	public static final String MQ_DELETED_LINKS_QUEUE;

	// Crontabs
	public static final String CRON_FOR_NEW_LINKS;
	public static final String CRON_FOR_RENEWED_LINKS;
	public static final String CRON_FOR_IMPLEMENTED_LINKS;
	public static final String CRON_FOR_AVAILABLE_LINKS;
	public static final String CRON_FOR_NOT_AVAILABLE_LINKS;
	public static final String CRON_FOR_SOCKET_ERRORS;
	public static final String CRON_FOR_NETWORK_ERRORS;
	public static final String CRON_FOR_RESUMED_LINKS;

	public static final String CRON_FOR_PRODUCT_PRICE_UPDATE;

	// DB
	public static final String DB_HOST;
	public static final int DB_PORT;
	public static final String DB_DATABASE;
	public static final String DB_USERNAME;
	public static final String DB_PASSWORD;
	public static final int DB_FETCH_LIMIT;

	// Redis
	public static final String REDIS_HOST;
	public static final int REDIS_PORT;
	public static final String REDIS_PASSWORD;

	// Waiting times
	public static final long WAITING_TIME_FOR_AWAIT_TERMINATION;
	public static final long WAITING_TIME_FOR_GETTING_LINKS_FROM_DB;

	// Retry limits
	public static final int RETRY_LIMIT_FOR_FAILED_LINKS_G1;
	public static final int RETRY_LIMIT_FOR_FAILED_LINKS_G2;
	public static final int RETRY_LIMIT_FOR_FAILED_LINKS_G3;

	static {
		MQ_HOST = getOrDefault("MQ_HOST", "localhost");
		MQ_PORT = getOrDefault("MQ_PORT", 5672);
		MQ_USERNAME = getOrDefault("MQ_USERNAME", "guest");
		MQ_PASSWORD = getOrDefault("MQ_PASSWORD", "guest");

		MQ_LINK_EXCHANGE = getOrDefault("MQ_LINK_EXCHANGE", "links");
		MQ_CHANGE_EXCHANGE = getOrDefault("MQ_CHANGE_EXCHANGE", "changes");

		//minutely
		MQ_NEW_LINKS_QUEUE = getOrDefault("MQ_NEW_LINKS_QUEUE", "new.links");
		MQ_FAILED_LINKS_QUEUE = getOrDefault("MQ_FAILED_LINKS_QUEUE", "failed.links");
		MQ_AVAILABLE_LINKS_QUEUE = getOrDefault("MQ_AVAILABLE_LINKS_QUEUE", "available.links");
		MQ_TOBE_AVAILABLE_LINKS_QUEUE = getOrDefault("MQ_TOBE_AVAILABLE_LINKS_QUEUE", "tobe-available.links");

		//different
		MQ_STATUS_CHANGE_QUEUE = getOrDefault("MQ_STATUS_CHANGE_QUEUE", "status.change");
		MQ_PRICE_CHANGE_QUEUE = getOrDefault("MQ_PRICE_CHANGE_QUEUE", "price.change");
		MQ_DELETED_LINKS_QUEUE = getOrDefault("MQ_DELETED_LINKS_QUEUE", "deleted.links");

		//minutely
		CRON_FOR_NEW_LINKS = getOrDefault("CRON_FOR_NEW_LINKS", "*/5 * * * * ?");
		CRON_FOR_SOCKET_ERRORS = getOrDefault("CRON_FOR_SOCKET_ERRORS", "*/5 * * * * ?");
		CRON_FOR_RESUMED_LINKS = getOrDefault("CRON_FOR_RESUMED_LINKS", "0 */13 * * * ?");
		CRON_FOR_RENEWED_LINKS = getOrDefault("CRON_FOR_RENEWED_LINKS", "*/8 * * * * ?");
		CRON_FOR_IMPLEMENTED_LINKS = getOrDefault("CRON_FOR_IMPLEMENTED_LINKS", "*/5 * * * * ?");

		//hourly
		CRON_FOR_PRODUCT_PRICE_UPDATE = getOrDefault("CRON_FOR_PRODUCT_PRICE_UPDATE", "*/11 * * * * ?");
		CRON_FOR_NETWORK_ERRORS = getOrDefault("CRON_FOR_NETWORK_ERRORS", "*/5 * * * * ?");
		CRON_FOR_AVAILABLE_LINKS = getOrDefault("CRON_FOR_AVAILABLE_LINKS", "*/5 * * * * ?");
		CRON_FOR_NOT_AVAILABLE_LINKS = getOrDefault("CRON_FOR_NOT_AVAILABLE_LINKS", "*/11 * * * * ?");

//		//minutely
//		CRON_FOR_NEW_LINKS = getOrDefault("CRON_FOR_NEW_LINKS", "0 */3 * * * ?");
//		CRON_FOR_SOCKET_ERRORS = getOrDefault("CRON_FOR_SOCKET_ERRORS", "0 */8 * * * ?");
//		CRON_FOR_RESUMED_LINKS = getOrDefault("CRON_FOR_RESUMED_LINKS", "0 */13 * * * ?");
//		CRON_FOR_RENEWED_LINKS = getOrDefault("CRON_FOR_RENEWED_LINKS", "0 */30 * * * ?");
//		CRON_FOR_IMPLEMENTED_LINKS = getOrDefault("CRON_FOR_IMPLEMENTED_LINKS", "0 */30 * * * ?");
//
//		//hourly
//		CRON_FOR_PRODUCT_PRICE_UPDATE = getOrDefault("CRON_FOR_PRODUCT_PRICE_UPDATE", "0 0 */1 * * ?");
//		CRON_FOR_NETWORK_ERRORS = getOrDefault("CRON_FOR_NETWORK_ERRORS", "0 0 */3 * * ?");
//		CRON_FOR_AVAILABLE_LINKS = getOrDefault("CRON_FOR_AVAILABLE_LINKS", "0 0 */6 * * ?");
//		CRON_FOR_NOT_AVAILABLE_LINKS = getOrDefault("CRON_FOR_NOT_AVAILABLE_LINKS", "0 0 */8 * * ?");

		DB_HOST = getOrDefault("DB_HOST", "localhost");
		DB_PORT = getOrDefault("DB_PORT", 3306);
		DB_DATABASE = getOrDefault("DB_DATABASE", "inprice");
		DB_USERNAME = getOrDefault("DB_USERNAME", "root");
		DB_PASSWORD = getOrDefault("DB_PASSWORD", "1234");
		DB_FETCH_LIMIT = getOrDefault("DB_FETCH_LIMIT", 100);

		REDIS_HOST = getOrDefault("REDIS_HOST", "localhost");
		REDIS_PORT = getOrDefault("REDIS_PORT", 6379);
		REDIS_PASSWORD = getOrDefault("REDIS_PASSWORD", null);

		WAITING_TIME_FOR_AWAIT_TERMINATION = getOrDefault("WTF_AWAIT_TERMINATION", 30000L);
		WAITING_TIME_FOR_GETTING_LINKS_FROM_DB = getOrDefault("WTF_GETTING_LINKS_FROM_DB", 3000L);

		RETRY_LIMIT_FOR_FAILED_LINKS_G1 = getOrDefault("RL_FAILED_LINKS_G1", 3);
		RETRY_LIMIT_FOR_FAILED_LINKS_G2 = getOrDefault("RL_FAILED_LINKS_G1", 5);
		RETRY_LIMIT_FOR_FAILED_LINKS_G3 = getOrDefault("RL_FAILED_LINKS_G3", 10);
	}

	private static String getOrDefault(String key, String defauld) {
		String val = System.getenv(key);
		if (val != null && val.trim().length() > 0) return val;
		return defauld;
	}

	private static int getOrDefault(String key, int defauld) {
		String val = System.getenv(key);
		if (val != null && val.trim().length() > 0) return Integer.parseInt(val.trim());
		return defauld;
	}

	private static long getOrDefault(String key, long defauld) {
		String val = System.getenv(key);
		if (val != null && val.trim().length() > 0) return Long.parseLong(val.trim());
		return defauld;
	}

}
