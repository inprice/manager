package io.inprice.manager.config;

import io.inprice.common.meta.LinkStatus;

public class Props {

  public static String TIME_PERIOD_OF(LinkStatus status) {
    return System.getenv().getOrDefault("TIME_PERIOD_OF_" + status.name(), "5s");
  }

  public static int RETRY_LIMIT_FOR(LinkStatus status) {
    return new Integer(System.getenv().getOrDefault("RETRY_LIMIT_FOR_" + status.name(), "3"));
  }

  public static String TIME_PERIOD_OF_REMOVING_MEMBERS() {
    return System.getenv().getOrDefault("TIME_PERIOD_OF_REMOVING_MEMBERS", "5s");
  }

  public static int DB_FETCH_LIMIT() {
    return new Integer(System.getenv().getOrDefault("DB_FETCH_LIMIT", "100"));
  }

  public static int WAITING_TIME_FOR_FETCHING_LINKS() {
    return new Integer(System.getenv().getOrDefault("WAITING_TIME_FOR_FETCHING_LINKS", "3"));
  }

}
