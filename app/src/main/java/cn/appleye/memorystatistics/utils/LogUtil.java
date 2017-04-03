package cn.appleye.memorystatistics.utils;

import android.util.Log;

/**
 * @author feiyu
 * @date 2017/4/3
 * 日志类
 */

public class LogUtil {
    public static boolean DEBUG = true;

    public static void d(String tag, String message) {
        if(DEBUG){
            Log.d(tag, message);
        }
    }
}
