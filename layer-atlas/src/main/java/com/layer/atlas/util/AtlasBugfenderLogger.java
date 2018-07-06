package com.layer.atlas.util;

import android.app.Application;
import android.content.Context;

import com.bugfender.sdk.Bugfender;
import com.bugfender.sdk.LogLevel;

public class AtlasBugfenderLogger {

    private AtlasBugfenderLogger(){

    }

    public static void init(Context context, String token, boolean debug){
        Bugfender.init(context, token, debug);
    }

    public static void enableUIEventLogging(Application application) {
        Bugfender.enableUIEventLogging(application);
    }

    public static void log(String message) {
        Bugfender.log(1, "", "", LogLevel.Debug, "chat", message);
    }
}
