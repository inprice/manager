package io.inprice.scrapper.manager.external;

public class Props {

  public static boolean IS_RUN_FOR_DEV() {
    return !"prod".equals(System.getenv().getOrDefault("APP_ENV", "prod").toLowerCase());
  }

  public static int WAITING_TIME_FOR_TERMINATION() {
    return new Integer(System.getenv().getOrDefault("WAITING_TIME_FOR_TERMINATION", "30"));
  }

  public static int WAITING_TIME_FOR_FETCHING_LINKS() {
    return new Integer(System.getenv().getOrDefault("WAITING_TIME_FOR_FETCHING_LINKS", "3"));
  }

  public static String DB_DRIVER() {
    String def = IS_RUN_FOR_DEV() ? "h2" : "mysql";
    return System.getenv().getOrDefault("DB_DRIVER", def);
  }

  public static String DB_HOST() {
    String def = IS_RUN_FOR_DEV() ? "mem" : "//localhost";
    return System.getenv().getOrDefault("DB_HOST", def);
  }

  public static int DB_PORT() {
    return new Integer(System.getenv().getOrDefault("DB_PORT", "3306"));
  }

  public static String DB_DATABASE() {
    String def = IS_RUN_FOR_DEV() ? "test" : "inprice";
    return System.getenv().getOrDefault("DB_DATABASE", def);
  }

  public static String DB_USERNAME() {
    String def = IS_RUN_FOR_DEV() ? "sa" : "root";
    return System.getenv().getOrDefault("DB_USERNAME", def);
  }

  public static String DB_PASSWORD() {
    String def = IS_RUN_FOR_DEV() ? "" : "1234";
    return System.getenv().getOrDefault("DB_PASSWORD", def);
  }

  public static String DB_ADDITIONS() {
    String def = IS_RUN_FOR_DEV()
        ? ";init=runscript from 'classpath:db/schema.sql'; runscript from 'classpath:db/data.sql'"
        : "";
    return System.getenv().getOrDefault("DB_ADDITIONS", def);
  }

  public static int DB_FETCH_LIMIT() {
    return new Integer(System.getenv().getOrDefault("DB_FETCH_LIMIT", "100"));
  }

  public static String REDIS_HOST() {
    return System.getenv().getOrDefault("REDIS_HOST", "localhost");
  }

  public static int REDIS_PORT() {
    return new Integer(System.getenv().getOrDefault("REDIS_PORT", "6379"));
  }

  public static String REDIS_PASSWORD() {
    return System.getenv().getOrDefault("REDIS_PASSWORD", null);
  }

  public static String MQ_HOST() {
    return System.getenv().getOrDefault("MQ_HOST", "localhost");
  }

  public static int MQ_PORT() {
    return new Integer(System.getenv().getOrDefault("MQ_PORT", "5672"));
  }

  public static String MQ_USERNAME() {
    return System.getenv().getOrDefault("MQ_USERNAME", "guest");
  }

  public static String MQ_PASSWORD() {
    return System.getenv().getOrDefault("MQ_PASSWORD", "guest");
  }

  public static String MQ_EXCHANGE_CHANGES() {
    return System.getenv().getOrDefault("MQ_EXCHANGE_CHANGES", "changes");
  }

  public static String MQ_EXCHANGE_LINKS() {
    return System.getenv().getOrDefault("MQ_EXCHANGE_LINKS", "links");
  }

  public static String MQ_EXCHANGE_DEAD_LETTER() {
    return System.getenv().getOrDefault("MQ_EXCHANGE_DEAD_LETTER", "dead-letters");
  }

  public static String MQ_ROUTING_NEW_LINKS() {
    return System.getenv().getOrDefault("MQ_ROUTING_NEW_LINKS", "new-links");
  }

  public static String MQ_ROUTING_AVAILABLE_LINKS() {
    return System.getenv().getOrDefault("MQ_ROUTING_AVAILABLE_LINKS", "available-links");
  }

  public static String MQ_ROUTING_FAILED_LINKS() {
    return System.getenv().getOrDefault("MQ_ROUTING_FAILED_LINKS", "failed-links");
  }

  public static String MQ_ROUTING_STATUS_CHANGES() {
    return System.getenv().getOrDefault("MQ_ROUTING_STATUS_CHANGES", "status-changes");
  }

  public static String MQ_QUEUE_STATUS_CHANGE() {
    return System.getenv().getOrDefault("MQ_QUEUE_STATUS_CHANGE", "status-change");
  }

  public static String MQ_QUEUE_PRICE_CHANGE() {
    return System.getenv().getOrDefault("MQ_QUEUE_PRICE_CHANGE", "price-change");
  }

  public static String MQ_QUEUE_DELETED_LINKS() {
    return System.getenv().getOrDefault("MQ_QUEUE_DELETED_LINKS", "deleted-links");
  }

  public static String MQ_QUEUE_TOBE_AVAILABLE_LINKS() {
    return System.getenv().getOrDefault("MQ_QUEUE_TOBE_AVAILABLE_LINKS", "tobe-available-links");
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
