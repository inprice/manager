package io.inprice.manager.config;

import io.inprice.common.utils.NumberUtils;

public class Props {

	public static final String APP_EMAIL_SENDER;

	public static final String API_KEYS_SENDGRID;
	
  public static final int DB_FETCH_LIMIT;
  public static final int WAITING_TIME_FOR_FETCHING_LINKS;

  public static final String INTERVAL_REMINDER_FOR_FREE_ACCOUNTS;
  public static final String INTERVAL_STOPPING_FREE_ACCOUNTS;
  public static final String INTERVAL_STOPPING_SUBSCRIBED_ACCOUNTS;
  public static final String INTERVAL_EXPIRING_PENDING_CHECKOUTS;
  public static final String INTERVAL_FLUSHING_ACCESS_LOG_QUEUE;

  static {
  	APP_EMAIL_SENDER = System.getenv().getOrDefault("APP_EMAIL_SENDER", "account@inprice.io");

  	API_KEYS_SENDGRID = System.getenv().get("API_KEYS_SENDGRID");

  	DB_FETCH_LIMIT = NumberUtils.toInteger(System.getenv().getOrDefault("DB_FETCH_LIMIT", "100"));
  	WAITING_TIME_FOR_FETCHING_LINKS = NumberUtils.toInteger(System.getenv().getOrDefault("WAITING_TIME_FOR_FETCHING_LINKS", "3"));

  	INTERVAL_REMINDER_FOR_FREE_ACCOUNTS = System.getenv().getOrDefault("INTERVAL_REMINDER_FOR_FREE_ACCOUNTS", "1d");
    INTERVAL_STOPPING_FREE_ACCOUNTS = System.getenv().getOrDefault("INTERVAL_STOPPING_FREE_ACCOUNTS", "1h");
  	INTERVAL_STOPPING_SUBSCRIBED_ACCOUNTS = System.getenv().getOrDefault("INTERVAL_STOPPING_SUBSCRIBED_ACCOUNTS", "57m");
  	INTERVAL_EXPIRING_PENDING_CHECKOUTS =  System.getenv().getOrDefault("INTERVAL_EXPIRING_PENDING_CHECKOUTS", "5m");
  	INTERVAL_FLUSHING_ACCESS_LOG_QUEUE =  System.getenv().getOrDefault("INTERVAL_FLUSHING_ACCESS_LOG_QUEUE", "5m");
  }

}
