package cn.appleye.memorystatistics;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.appleye.memorystatistics.common.model.AppInfo;
import cn.appleye.memorystatistics.utils.LogUtil;
import cn.appleye.memorystatistics.utils.ProcessUtil;

public class MemoryStatisticsActivity extends AppCompatActivity {
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

    /**上一次的进程名*/
    private String mLastProcessName;

    private List<AppInfo> mAppInfoList;

    /**进程列表弹出窗*/
    private PopupWindow mProcessPopupWindow;
    /**进程列表*/
    private ListView mProcessListView;
    private ProcessListAdapter mProcessAdapter;

    private Resources mResources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_statistics);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mResources = getResources();

        ButterKnife.bind(this);

        obtainAppList();
    }

    /**
     * 获取应用列表
     * */
    private void obtainAppList() {
        mAppInfoList = ProcessUtil.getInstalledApp(this);
        if(mAppInfoList != null) {
            LogUtil.d(TAG, "app size = " + mAppInfoList.size());
        }
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
    void startTraceMemoryStatis() {
        String processName = mProcessInputView.getText().toString();
        if(TextUtils.isEmpty(processName)) {
            Toast.makeText(this, R.string.toast_process_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if(TextUtils.equals(processName, mLastProcessName)) {
            return;
        }

        mLastProcessName = processName;
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

            viewHolder.processNameView.setText(appInfo.mPackageName);

            return convertView;
        }
    }

}
