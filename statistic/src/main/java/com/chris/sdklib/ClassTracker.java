package com.chris.sdklib;

import java.util.Arrays;

/**
 * Created by chris on 2017/12/25.
 */

public class ClassTracker {

    public static void onEvent(Object view) {
        System.out.println(view);
    }

    public static void onEvents(Object... objects) {
        System.out.println(Arrays.toString(objects));
    }
}
