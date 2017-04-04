package cn.appleye.memorystatistics.statisic;

/**
 * @author feiyu
 * @date 2017/4/4
 * 内存变化观察者
 */

public interface StatisticObserver {
    /**
     * 获取要统计的数据
     * */
    String getTargetName();
    /**
     * 数据获取到时回调
     * */
    void onDataObtained(String name, int value);
}
