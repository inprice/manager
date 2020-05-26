package io.inprice.scrapper.manager.helpers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Global {

  public static volatile boolean isApplicationRunning;
  private static volatile Map<String, Boolean> runningTasksMap = new ConcurrentHashMap<>();

  public static void setTaskRunningStatus(String taskName, boolean status) {
    runningTasksMap.put(taskName, status);
  }

  public static boolean isTaskRunning(String taskName) {
    Boolean result = runningTasksMap.get(taskName);
    if (result != null)
      return result;
    return false;
  }

}
