package io.inprice.manager.config;

import io.inprice.common.utils.NumberUtils;

public class Props {

  public static final int DB_FETCH_LIMIT;
  public static final int WAITING_TIME_FOR_FETCHING_LINKS;
	
  static {
  	DB_FETCH_LIMIT = NumberUtils.toInteger(System.getenv().getOrDefault("DB_FETCH_LIMIT", "100"));
  	WAITING_TIME_FOR_FETCHING_LINKS = NumberUtils.toInteger(System.getenv().getOrDefault("WAITING_TIME_FOR_FETCHING_LINKS", "3"));
  }

}
