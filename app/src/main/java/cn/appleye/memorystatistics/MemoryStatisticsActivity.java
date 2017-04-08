package cn.appleye.memorystatistics;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.appleye.memorystatistics.common.model.AppInfo;
import cn.appleye.memorystatistics.statisic.StatisticObserver;
import cn.appleye.memorystatistics.statisic.StatisticsManager;
import cn.appleye.memorystatistics.utils.LogUtil;
import cn.appleye.memorystatistics.utils.ProcessUtil;
import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * @author liuliaopu
 * @date 2017/4/3
 * */
public class MemoryStatisticsActivity extends AppCompatActivity implements StatisticObserver {
    private static final String TAG = "MemoryStatistic";

    /**进程名称输入空间*/
    @BindView(R.id.process_input_view)
    EditText mProcessInputView;

    /**下拉显示运行的进程名*/
    @BindView(R.id.drop_down)
    View mDropDownView;

    /**开始监听进程内存变化按钮*/
    @BindView(R.id.confirm_btn)
    Button mConfirmBtn;

    /**图表控件*/
    @BindView(R.id.chart)
    LineChartView mLineChart;

    /**上一次的进程名*/
    private String mLastProcessName;

    /**列表显示的应用列表*/
    private List<AppInfo> mAppInfoList;
    /**所有应用列表*/
    private List<AppInfo> mTotalAppInfoList;

    /**进程列表弹出窗*/
    private PopupWindow mProcessPopupWindow;
    /**进程列表*/
    private ListView mProcessListView;
    private ProcessListAdapter mProcessAdapter;

    private Resources mResources;

    private StatisticsManager mStatisticsManager;

    private List<PointValue> mPointValues = new ArrayList<PointValue>();
    private List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();

    private LineChartData mLineChartData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_statistics);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mResources = getResources();

        mStatisticsManager = StatisticsManager.getInstance();
        mStatisticsManager.registerObserver(this);

        ButterKnife.bind(this);

        initLineChart();

        obtainAppList();
    }

    /**
     * 初始化图表
     * */
    private void initLineChart(){
        Line line = new Line(mPointValues).setColor(Color.parseColor("#FFCD41"));  //折线的颜色（橙色）
        List<Line> lines = new ArrayList<Line>();
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
        line.setCubic(true);//曲线是否平滑，即是曲线还是折线
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(false);//曲线的数据坐标是否加上备注
//      line.setHasLabelsOnlyForSelected(true);//点击数据坐标提示数据（设置了这个line.setHasLabels(true);就无效）
        line.setHasLines(true);//是否用线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(false);//是否显示圆点 如果为false 则没有原点只有点显示（每个数据点都是个大的圆点）
        lines.add(line);
        mLineChartData = new LineChartData();
        mLineChartData.setLines(lines);

        //坐标轴
        Axis axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(true);  //X坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.WHITE);  //设置字体颜色
        //axisX.setName("date");  //表格名称
        axisX.setTextSize(10);//设置字体大小
        axisX.setMaxLabelChars(8); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisXValues.length
        axisX.setValues(mAxisXValues);  //填充X轴的坐标名称
        mLineChartData.setAxisXBottom(axisX); //x 轴在底部
        //data.setAxisXTop(axisX);  //x 轴在顶部
        axisX.setHasLines(true); //x 轴分割线

        // Y轴是根据数据的大小自动设置Y轴上限(在下面我会给出固定Y轴数据个数的解决方案)
        Axis axisY = new Axis();  //Y轴
        axisY.setName("");//y轴标注
        axisY.setTextSize(10);//设置字体大小
        mLineChartData.setAxisYLeft(axisY);  //Y轴设置在左边
        //data.setAxisYRight(axisY);  //y轴设置在右边

        //设置行为属性，支持缩放、滑动以及平移
        mLineChart.setInteractive(true);
        mLineChart.setZoomType(ZoomType.HORIZONTAL);
        mLineChart.setMaxZoom((float) 2);//最大方法比例
        mLineChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        mLineChart.setLineChartData(mLineChartData);
        mLineChart.setVisibility(View.VISIBLE);
    }


    /**
     * 获取应用列表
     * */
    private void obtainAppList() {
        mTotalAppInfoList = ProcessUtil.getInstalledApp(this);
        if(mTotalAppInfoList != null) {
            LogUtil.d(TAG, "app size = " + mTotalAppInfoList.size());
        }

        /*排序*/
        Collections.sort(mTotalAppInfoList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo appInfo1, AppInfo appInfo2) {
                if(appInfo1.mPackageName == null) {
                    return -1;
                }
                return appInfo1.mPackageName.compareTo(appInfo2.mPackageName);
            }
        });
        //初始时，让listview显示所有应用
        mAppInfoList = mTotalAppInfoList;
    }

    /**
     * 显示进程列表
     * */
    @OnClick(R.id.drop_down)
    void showDropDownListView() {
        if(mProcessPopupWindow == null) {

            LayoutInflater inflater = getLayoutInflater();
            View contentView = inflater.inflate(R.layout.process_popup_window, null);
            mProcessListView = (ListView) contentView;

            int width = mProcessInputView.getMeasuredWidth();
            int height = mResources.getDimensionPixelOffset(R.dimen.popup_window_height);
            mProcessPopupWindow = new PopupWindow(contentView, width, height);

            mProcessPopupWindow.setTouchInterceptor(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    LogUtil.d(TAG, "onTouch : ");

                    return false;
                    // 这里如果返回true的话，touch事件将被拦截
                    // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
                }
            });
            mProcessPopupWindow.setBackgroundDrawable(
                    mResources.getDrawable(R.drawable.round_rectangle_bg));
            mProcessPopupWindow.setTouchable(true);
            mProcessPopupWindow.setOutsideTouchable(true);

            mProcessAdapter = new ProcessListAdapter();
            mProcessListView.setAdapter(mProcessAdapter);
            mProcessListView.setVerticalScrollBarEnabled(false);
            mProcessListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    AppInfo appInfo = (AppInfo)mProcessAdapter.getItem(position);
                    mProcessInputView.setText(appInfo.mPackageName);
                    mProcessInputView.setSelection(appInfo.mPackageName.length());

                    mProcessPopupWindow.dismiss();
                }
            });
        }

        mProcessPopupWindow.showAsDropDown(mProcessInputView);
    }

    /**
     * 开始监听当前进程的占用内存状态
     * */
    @OnClick(R.id.confirm_btn)
    void startTraceMemoryStatistic() {
        String processName = mProcessInputView.getText().toString();
        if(TextUtils.isEmpty(processName)) {
            Toast.makeText(this, R.string.toast_process_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if(TextUtils.equals(processName, mLastProcessName)) {
            return;
        }

        LogUtil.d(TAG, "[startTraceMemoryStatistic] processName = " + processName);

        mLastProcessName = processName;

        mPointValues.clear();
        mAxisXValues.clear();

        mStatisticsManager.execute();
    }

    /**
     * 输入框内容变化控件
     * */
    @OnTextChanged(R.id.process_input_view)
    void onProcessTextChanged(CharSequence text) {
        if(TextUtils.isEmpty(text)) {
            mAppInfoList = mTotalAppInfoList;
        } else {
            if(mAppInfoList == mTotalAppInfoList) {
                mAppInfoList = new ArrayList<>();
            }

            mAppInfoList.clear();
            for(AppInfo appInfo : mTotalAppInfoList) {
                if(appInfo.mPackageName != null && appInfo.mPackageName.contains(text)){
                    mAppInfoList.add(appInfo);
                }
            }
        }

        if(mProcessAdapter != null) {
            mProcessAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        /*返回按钮优先消失对话框*/
        if(mProcessPopupWindow != null && mProcessPopupWindow.isShowing()) {
            mProcessPopupWindow.dismiss();
            return;
        }

        super.onBackPressed();
    }

    @Override
    public String getTargetName() {
        return mLastProcessName;
    }

    @Override
    public void onDataObtained(String name, int value) {
        LogUtil.d(TAG, "[onDataObtained] name = " + name + ", value = " + value + "KB");
        Calendar calendar = Calendar.getInstance();
        String date = calendar.get(Calendar.HOUR_OF_DAY) + ":"
                + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND);
        mAxisXValues.add(new AxisValue(mAxisXValues.size()).setLabel(date));
        mPointValues.add(new PointValue(mAxisXValues.size()-1, value/1024.0f));

        mLineChart.setLineChartData(mLineChartData);
    }

    public void onDestroy() {
        super.onDestroy();
        mStatisticsManager.unregisterObserver(this);
    }


    private static class ProcessViewHolder{
        TextView appLabelView;
        TextView processNameView;
    }

    /**
     * 下拉列表适配器
     * */
    private class ProcessListAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return mAppInfoList==null ? 0:mAppInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            return mAppInfoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ProcessViewHolder viewHolder;
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.process_list_item_view, null);
                viewHolder = new ProcessViewHolder();
                viewHolder.appLabelView = (TextView) convertView.findViewById(R.id.app_label_view);
                viewHolder.processNameView = (TextView) convertView.findViewById(R.id.process_name_view);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ProcessViewHolder) convertView.getTag();
            }

            AppInfo appInfo = (AppInfo)getItem(position);
            if(TextUtils.isEmpty(appInfo.mLabel)) {
                viewHolder.appLabelView.setVisibility(View.GONE);
            } else {
                viewHolder.appLabelView.setText(appInfo.mLabel);
                viewHolder.appLabelView.setVisibility(View.VISIBLE);
            }

            String packageName = appInfo.mPackageName;

            String processValue = mProcessInputView.getText().toString();

            //高亮
            if(packageName != null && packageName.contains(processValue)) {
                int startIndex = packageName.indexOf(processValue);
                int endIndex = startIndex + processValue.length();
                SpannableString sp = new SpannableString(packageName);
                sp.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blue)),
                        startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                viewHolder.processNameView.setText(sp);
            } else {
                viewHolder.processNameView.setText(packageName);
            }


            return convertView;
        }
    }

}
