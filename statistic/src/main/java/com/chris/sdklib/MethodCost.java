package com.chris.sdklib;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by chris on 2017/12/22.
 */

public class MethodCost {
    /**
     * time is nano-time
     */
    private static final long NANO_TO_MS = 1000 * 1000;
    private static Map<String, Long> timeStart = new HashMap<>();
    private static Map<String, Long> timeEnd = new HashMap<>();

    public static void setTimeStart(String method, long time) {
        timeStart.put(method, time);
    }

    public static void setTimeEnd(String method, long time) {
        timeEnd.put(method, time);
    }

    public static String getTimeCost(String method) {
        long nano = timeEnd.remove(method) - timeStart.remove(method);
        long cost = nano / NANO_TO_MS;
        return String.format(Locale.getDefault(), "[%s] cost %dms/%dns", method, cost, nano);
    }
}
