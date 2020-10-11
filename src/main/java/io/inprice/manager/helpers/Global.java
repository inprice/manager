package io.inprice.manager.helpers;

import java.util.HashSet;
import java.util.Set;

public class Global {

  public static volatile boolean isApplicationRunning;

  private static volatile Set<String> runningTasksSet = new HashSet<>();

  public static void startTask(String name) {
    runningTasksSet.add(name);
  }

  public static void stopTask(String name) {
    runningTasksSet.remove(name);
  }

  public static boolean isTaskRunning(String name) {
    return runningTasksSet.contains(name);
  }

}
