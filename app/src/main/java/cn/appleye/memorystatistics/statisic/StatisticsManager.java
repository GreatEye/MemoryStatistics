package cn.appleye.memorystatistics.statisic;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import cn.appleye.memorystatistics.utils.LogUtil;
import cn.appleye.memorystatistics.utils.ProcessUtil;

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author feiyu
 * @date 2017/4/4
 */

public class StatisticsManager {
    private static final String TAG = "StatisticsManager";

    private static volatile StatisticsManager sInstance;

    private static Context sContext;

    private HashSet<StatisticObserver> mObservers;

    /**
     * 主线程handler
     * */
    private Handler mMainHandler;

    private HandlerThread mHandlerThread;
    private Handler mWorkerHandler;

    /**执行间隔2s*/
    private static final int SCHEDULE_DELAY = 2000;

    /**请求获取内存数据*/
    private static final int MSG_MEM_QUERY = 1000;
    /**获取到内存数据*/
    private static final int MSG_MEM_OBTAINED = 1001;

    private StatisticsManager(){
        mObservers = new HashSet<>();

        mMainHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_MEM_OBTAINED :{
                        HashMap<String, Integer> processMemInfos = (HashMap<String, Integer>)msg.obj;

                        if(processMemInfos != null) {
                            Iterator<StatisticObserver> iterator = mObservers.iterator();
                            while(iterator.hasNext()) {
                                StatisticObserver observer = iterator.next();
                                String name = observer.getTargetName();
                                Integer value = processMemInfos.get(name);
                                if(value != null) {
                                    observer.onDataObtained(name, value);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        };
    }

    public static void install(Application application) {
        sContext = application;
    }

    /**
     * 获取单例
     * */
    public static StatisticsManager getInstance() {
        if(sContext == null) {
            throw new IllegalArgumentException("please call install first");
        }

        if(sInstance == null) {
            synchronized (StatisticsManager.class) {
                if(sInstance == null) {
                    sInstance = new StatisticsManager();
                }
            }
        }

        return sInstance;
    }

    /**
     * 注册观察者，使用完调用注销，不然出现内存泄露
     * @param observer 观察者
     * */
    public void registerObserver(StatisticObserver observer) {
        mObservers.add(observer);
    }

    /**
     * 注销观察者
     * */
    public void unregisterObserver(StatisticObserver observer) {
        mObservers.remove(observer);

        if(mObservers.size() == 0) {
            stop();
        }
    }

    /**
     * 开始执行
     * */
    public void execute() {
        if(mObservers.size() == 0) {//没有要监听的对象，执行无意义
            return;
        }

        if(mWorkerHandler == null) {
            if(mHandlerThread == null) {
                mHandlerThread = new HandlerThread("memory-statistic");
                mHandlerThread.start();
            }

            mWorkerHandler = new Handler(mHandlerThread.getLooper()){
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_MEM_QUERY : {
                            HashMap<String, Integer> processMemInfo = ProcessUtil.getTaskInfos(sContext);
                            Message memMsg = Message.obtain();
                            memMsg.what = MSG_MEM_OBTAINED;
                            memMsg.obj = processMemInfo;
                            mMainHandler.sendMessage(memMsg);

                            mWorkerHandler.sendEmptyMessageDelayed(MSG_MEM_QUERY, SCHEDULE_DELAY);
                            break;
                        }
                    }
                }
            };

            mWorkerHandler.sendEmptyMessageDelayed(MSG_MEM_QUERY, SCHEDULE_DELAY);
        }
    }

    public void stop() {
        if(mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }

        mWorkerHandler = null;
    }

}
