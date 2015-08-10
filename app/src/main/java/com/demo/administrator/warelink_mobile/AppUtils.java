package com.demo.administrator.warelink_mobile;

import android.app.Activity;
import android.util.Log;

import com.gpd.sdk.service.GpdService;
import com.gpd.sdk.util.GpdUtils;

/**
 * Created by Administrator on 2015/5/24.
 */
public class AppUtils {
    public static void startService(Activity context, int device) {
        GpdService.startService(context, device);
        GpdService.setDebugServiceFlag(true);
    }

    public static void stopService() {
        GpdService.stopService();

        GpdUtils.destroySoundPool();
        Log.v("ref app", "stop Service");
    }

    /**
     * app service related
     */
    // can add app global functions here...
}
