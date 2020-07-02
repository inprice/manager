package io.inprice.manager.config;

public class Props {

  public static int WAITING_TIME_FOR_FETCHING_COMPETITORS() {
    return new Integer(System.getenv().getOrDefault("WAITING_TIME_FOR_FETCHING_COMPETITORS", "3"));
  }

  public static int DB_FETCH_LIMIT() {
    return new Integer(System.getenv().getOrDefault("DB_FETCH_LIMIT", "100"));
  }

  public static String TIMING_FOR_TOBE_CLASSIFIED_COMPETITORS() {
    return System.getenv().getOrDefault("TIMING_FOR_TOBE_CLASSIFIED_COMPETITORS", "3M");
  }

  public static String TIMING_FOR_AVAILABLE_COMPETITORS() {
    return System.getenv().getOrDefault("TIMING_FOR_AVAILABLE_COMPETITORS", "6H");
  }

  public static String TIMING_FOR_NOT_AVAILABLE_COMPETITORS() {
    return System.getenv().getOrDefault("TIMING_FOR_NOT_AVAILABLE_COMPETITORS", "8H");
  }

  public static String TIMING_FOR_RESUMED_COMPETITORS() {
    return System.getenv().getOrDefault("TIMING_FOR_RESUMED_COMPETITORS", "13M");
  }

  public static String TIMING_FOR_TOBE_RENEWED_COMPETITORS() {
    return System.getenv().getOrDefault("TIMING_FOR_TOBE_RENEWED_COMPETITORS", "27M");
  }

  public static String TIMING_FOR_IMPLEMENTED_COMPETITORS() {
    return System.getenv().getOrDefault("TIMING_FOR_IMPLEMENTED_COMPETITORS", "33M");
  }

  public static String TIMING_FOR_UPDATING_PRODUCT_PRICES() {
    return System.getenv().getOrDefault("TIMING_FOR_UPDATING_PRODUCT_PRICES", "1H");
  }

  public static String TIMING_FOR_CLEANING_COMPETITORS() {
    return System.getenv().getOrDefault("TIMING_FOR_CLEANING_COMPETITORS", "1d");
  }

  public static String TIMING_FOR_SOCKET_ERRORS() {
    return System.getenv().getOrDefault("TIMING_FOR_SOCKET_ERRORS", "8M");
  }

  public static String TIMING_NO_DATA_ERRORS() {
    return System.getenv().getOrDefault("TIMING_NO_DATA_ERRORS", "10M");
  }

  public static String TIMING_FOR_NETWORK_ERRORS() {
    return System.getenv().getOrDefault("TIMING_FOR_NETWORK_ERRORS", "3H");
  }

  public static String TIMING_FOR_BLOCKED_COMPETITORS() {
    return System.getenv().getOrDefault("TIMING_FOR_BLOCKED_COMPETITORS", "5H");
  }

  public static int RETRY_LIMIT_FOR_FAILED_COMPETITORS_G1() {
    return new Integer(System.getenv().getOrDefault("RETRY_LIMIT_FOR_FAILED_COMPETITORS_G1", "3"));
  }

  public static int RETRY_LIMIT_FOR_FAILED_COMPETITORS_G2() {
    return new Integer(System.getenv().getOrDefault("RETRY_LIMIT_FOR_FAILED_COMPETITORS_G2", "5"));
  }

  public static int RETRY_LIMIT_FOR_FAILED_COMPETITORS_G3() {
    return new Integer(System.getenv().getOrDefault("RETRY_LIMIT_FOR_FAILED_COMPETITORS_G3", "10"));
  }

}
