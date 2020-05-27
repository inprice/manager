package io.inprice.scrapper.manager.config;

public class Props {

  public static int WAITING_TIME_FOR_FETCHING_LINKS() {
    return new Integer(System.getenv().getOrDefault("WAITING_TIME_FOR_FETCHING_LINKS", "3"));
  }

  public static int DB_FETCH_LIMIT() {
    return new Integer(System.getenv().getOrDefault("DB_FETCH_LIMIT", "100"));
  }

  public static String TIMING_FOR_NEW_LINKS() {
    return System.getenv().getOrDefault("TIMING_FOR_NEW_LINKS", "3M");
  }

  public static String TIMING_FOR_AVAILABLE_LINKS() {
    return System.getenv().getOrDefault("TIMING_FOR_AVAILABLE_LINKS", "6H");
  }

  public static String TIMING_FOR_NOT_AVAILABLE_LINKS() {
    return System.getenv().getOrDefault("TIMING_FOR_NOT_AVAILABLE_LINKS", "8H");
  }

  public static String TIMING_FOR_RESUMED_LINKS() {
    return System.getenv().getOrDefault("TIMING_FOR_RESUMED_LINKS", "13M");
  }

  public static String TIMING_FOR_RENEWED_LINKS() {
    return System.getenv().getOrDefault("TIMING_FOR_RENEWED_LINKS", "27M");
  }

  public static String TIMING_FOR_IMPLEMENTED_LINKS() {
    return System.getenv().getOrDefault("TIMING_FOR_IMPLEMENTED_LINKS", "33M");
  }

  public static String TIMING_FOR_UPDATING_PRODUCT_PRICES() {
    return System.getenv().getOrDefault("TIMING_FOR_UPDATING_PRODUCT_PRICES", "1H");
  }

  public static String TIMING_FOR_CLEANING_LINKS() {
    return System.getenv().getOrDefault("TIMING_FOR_CLEANING_LINKS", "1d");
  }

  public static String TIMING_FOR_SOCKET_ERRORS() {
    return System.getenv().getOrDefault("TIMING_FOR_SOCKET_ERRORS", "8M");
  }

  public static String TIMING_FOR_NETWORK_ERRORS() {
    return System.getenv().getOrDefault("TIMING_FOR_NETWORK_ERRORS", "3H");
  }

  public static int RETRY_LIMIT_FOR_FAILED_LINKS_G1() {
    return new Integer(System.getenv().getOrDefault("RETRY_LIMIT_FOR_FAILED_LINKS_G1", "3"));
  }

  public static int RETRY_LIMIT_FOR_FAILED_LINKS_G2() {
    return new Integer(System.getenv().getOrDefault("RETRY_LIMIT_FOR_FAILED_LINKS_G2", "5"));
  }

  public static int RETRY_LIMIT_FOR_FAILED_LINKS_G3() {
    return new Integer(System.getenv().getOrDefault("RETRY_LIMIT_FOR_FAILED_LINKS_G3", "10"));
  }

}
