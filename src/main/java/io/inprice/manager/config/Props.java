package io.inprice.manager.config;

import io.inprice.common.meta.LinkStatus;
import io.inprice.common.utils.NumberUtils;

public class Props {

  public static String COLLECTING_PERIOD_OF(LinkStatus status) {
    return System.getenv().getOrDefault("COLLECTING_PERIOD_OF_" + status.name(), "15s");
  }

  public static int RETRY_LIMIT_FOR(LinkStatus status) {
    return new Integer(System.getenv().getOrDefault("RETRY_LIMIT_FOR_" + status.name(), "3"));
  }

  public static int INTERVAL_FOR_LINK_COLLECTION() {
    return new Integer(System.getenv().getOrDefault("INTERVAL_FOR_LINK_COLLECTION", "30"));
  }

  public static String TIME_PERIOD_OF_REMOVING_MEMBERS() {
    return System.getenv().getOrDefault("TIME_PERIOD_OF_REMOVING_MEMBERS", "3h");
  }

  public static String TIME_PERIOD_OF_INACTIVATING_LINKS() {
    return System.getenv().getOrDefault("TIME_PERIOD_OF_INACTIVATING_LINKS", "1h");
  }

  public static int DB_FETCH_LIMIT() {
    return new Integer(System.getenv().getOrDefault("DB_FETCH_LIMIT", "100"));
  }

  public static int WAITING_TIME_FOR_FETCHING_LINKS() {
    return new Integer(System.getenv().getOrDefault("WAITING_TIME_FOR_FETCHING_LINKS", "3"));
  }

  public static int STATUS_CHANGE_CONSUMER_TPOOL_CAPACITY() {
    return NumberUtils.toInteger(System.getenv().getOrDefault("STATUS_CHANGE_CONSUMER_TPOOL_CAPACITY", "4"));
  }

}
