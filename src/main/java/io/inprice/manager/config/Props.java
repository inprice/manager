package io.inprice.manager.config;

import io.inprice.common.meta.LinkStatus;
import io.inprice.common.utils.NumberUtils;

public class Props {

  public static int DB_FETCH_LIMIT() {
    return new Integer(System.getenv().getOrDefault("DB_FETCH_LIMIT", "100"));
  }

  public static int WAITING_TIME_FOR_FETCHING_LINKS() {
    return new Integer(System.getenv().getOrDefault("WAITING_TIME_FOR_FETCHING_LINKS", "3"));
  }

}
