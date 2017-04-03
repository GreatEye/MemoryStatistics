package cn.appleye.memorystatistics.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.appleye.memorystatistics.common.model.AppInfo;

/**
 * @author liuliaopu
 * @date 2017-04-02
 */
public class ProcessUtil {

    /**
     * 获取运行的进程
     * @param context 上下文菜单
     * @return {进程名，占用内存(Pss)}
     * */
    public static HashMap<String, Integer> getTaskInfos(Context context)  {
        //首先获取到进程管理器
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //获取到运行的进程
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        //初始化进程的集合
        HashMap<String, Integer> tasks = new HashMap<>();
        //迭代所有正在运行的进程
        for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
            //获取到进程的名字
            String processName = runningAppProcess.processName;
            //获取到进程的内存的基本信息
            Debug.MemoryInfo[] processMemoryInfo = activityManager.getProcessMemoryInfo(new int[]{runningAppProcess.pid});
            int totalPrivateDirty = processMemoryInfo[0].getTotalPrivateDirty() * 1024;
            tasks.put(processName, totalPrivateDirty);
        }
        return tasks;
    }


    /**
     * 获取所有已经安装了的应用列表
     * @param context 上下文
     * */
    public static List<AppInfo> getInstalledApp(Context context) {
        List<AppInfo> appInfos = new ArrayList<>();
        try{
            PackageManager pm = context.getPackageManager();
            List<PackageInfo> packageInfos = pm.getInstalledPackages(0);
            for(PackageInfo packageInfo : packageInfos) {
                AppInfo appInfo = new AppInfo();
                appInfo.mPackageName = packageInfo.packageName;
                appInfo.mLabel = packageInfo.applicationInfo.loadLabel(pm).toString();
                appInfos.add(appInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return appInfos;
    }
}
