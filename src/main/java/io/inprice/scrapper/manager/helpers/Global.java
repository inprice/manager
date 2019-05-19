package io.inprice.scrapper.manager.helpers;

import io.inprice.scrapper.common.meta.Status;

import java.util.HashMap;
import java.util.Map;

public class Global {

    public static volatile boolean isApplicationRunning;
    public static volatile boolean isProductUpdaterRunning;

    public static Map<Status, Integer> statusCycleMap = new HashMap<>();

}
