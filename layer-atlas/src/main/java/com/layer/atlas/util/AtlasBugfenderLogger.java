package com.layer.atlas.util;

import com.bugfender.sdk.Bugfender;
import com.bugfender.sdk.LogLevel;

public class AtlasBugfenderLogger {

    private AtlasBugfenderLogger(){

    }

    public static void log(String message) {
        Bugfender.log(1, "", "", LogLevel.Debug, "chat", message);
    }
}
