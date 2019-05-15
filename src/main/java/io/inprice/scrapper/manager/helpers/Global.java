package io.inprice.scrapper.manager.helpers;

import io.inprice.scrapper.common.meta.LinkStatus;

import java.util.HashMap;
import java.util.Map;

public class Global {

    public static volatile boolean isRunning;

    public static Map<LinkStatus, Integer> linkStatusCycleMap = new HashMap<>();

}
