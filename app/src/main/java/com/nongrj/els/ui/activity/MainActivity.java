package com.nongrj.els.ui.activity;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.just.agentweb.PermissionInterceptor;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.nongrj.els.R;
import com.nongrj.els.main.activitys.BaseAgentWebViewActivity;
import com.nongrj.els.main.app.Constant;
import com.nongrj.els.ui.bean.BluetoothBean;
import com.nongrj.els.ui.interfaces.JSNativeInterface;
import com.nongrj.els.utils.ToolsUtils;
import com.nongrj.els.widget.MaterialDialog;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import io.reactivex.functions.Consumer;

public class MainActivity extends BaseAgentWebViewActivity {

    private static String SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb";
    //    private TextView mTitleTextView;
    private MaterialDialog mPermissionDialog;

    //选择图片
    private List<LocalMedia> selectList = new ArrayList<>();
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;

    private String data;  //打印数据

    //蓝牙
    private BluetoothSocket socket;//蓝牙socket
    private ConnectThread mThread;//连接的蓝牙线程
    private MyBroadcastReceiver receiver;//蓝牙搜索的广播
    private BluetoothAdapter adapter;//蓝牙适配器
    private List<BluetoothBean> mBluetoothList;//搜索的蓝牙设备
    private List<BluetoothBean> mBluetoothList2;//去重的蓝牙设备
    private PopupWindow pw;
    private String TAG = "FolloMountains_els";
    private ProgressDialog pdSearch;//搜索时
    private ProgressDialog pdConnect;//连接时

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        mTitleTextView = findViewById(R.id.mTitleTextView);

        mPermissionDialog = new MaterialDialog(this);

//        ActionBar actionBar = getSupportActionBar();
//        assert actionBar != null;
//        actionBar.setDisplayShowTitleEnabled(false);
//        mTitleTextView.setText("字数要一样");

        mAgentWeb.getJsInterfaceHolder().addJavaObject("android", new JSNativeInterface(MainActivity.this));

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                selectImage();
////                open();
////                Intent i = new Intent(MainActivity.this, CaptureActivity.class);
////                startActivityForResult(i, Constant.REQUEST_QR_CODE);
////                searchBlueToothDevice();
//
//                mAgentWeb.getJsAccessEntrace().quickCallJs("getData");
//
//            }
//        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case android.R.id.home:
//                if (mAgentWeb.back()) {
//                    mAgentWeb.getIEventHandler().back();
//                } else {
//                    finish();
//                }
//
//                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    protected ViewGroup getAgentWebParent() {
        return (ViewGroup) this.findViewById(R.id.ll_content);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAgentWeb != null && mAgentWeb.handleKeyEvent(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void setTitle(WebView view, String title) {

    }

    @Override
    protected int getIndicatorColor() {
        return Color.parseColor("#ff0000");
    }

    @Override
    protected int getIndicatorHeight() {
        return 3;
    }

    @Nullable
    @Override
    protected String getUrl() {
        return Constant.URL;
    }


    /**
     * 图片选择 权限判断（目前无效 未找到原因）
     * <p>
     * web页面中input type="file"的事件监听在webview中使用onShowFileChooser实现
     */
    private void open() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.requestEachCombined(Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE).subscribe(new Consumer<Permission>() {
            @Override
            public void accept(Permission permission) throws Exception {
                if (permission.granted) {
                    // All permissions are granted !
                    selectImage();
                } else if (permission.shouldShowRequestPermissionRationale) {
                    // At least one denied permission without ask never again
                    Toast.makeText(MainActivity.this, getString(R.string.permission_request), Toast.LENGTH_LONG).show();
                } else {
                    // At least one denied permission with ask never again
                    // Need to go to the settings
                    mPermissionDialog.show();
                }
            }
        });


    }

    public void selectImage() {
        // 进入相册
        PictureSelector.create(MainActivity.this)
                .openGallery(PictureMimeType.ofImage())// 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
//                .maxSelectNum(1)// 最大图片选择数量
//                .minSelectNum(1)// 最小选择数量
                .imageSpanCount(4)// 每行显示个数
                .selectionMode(PictureConfig.SINGLE)// 多选 or 单选
                .previewImage(true)// 是否可预览图片
//                .previewVideo(false)// 是否可预览视频
//                .enablePreviewAudio(false) // 是否可播放音频
                .isCamera(true)// 是否显示拍照按钮
                .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                .imageFormat(PictureMimeType.PNG)// 拍照保存图片格式后缀,默认jpeg
                .setOutputCameraPath("/CustomPath")// 自定义拍照保存路径
//                .enableCrop(true)// 是否裁剪
                .compress(true)// 是否压缩
                .synOrAsy(true)//同步true或异步false 压缩 默认同步
                .compressSavePath(ToolsUtils.getCacheDirectory(MainActivity.this, Environment.DIRECTORY_PICTURES).getPath())//压缩图片保存地址
                .sizeMultiplier(0.5f)// glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
                .glideOverride(160, 160)// glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
//                .withAspectRatio(9, 9)// 裁剪比例 如16:9 3:2 3:4 1:1 可自定义
                .hideBottomControls(true)// 是否显示uCrop工具栏，默认不显示
//                .isGif(false)// 是否显示gif图片
//                .freeStyleCropEnabled(true)// 裁剪框是否可拖拽
//                .circleDimmedLayer(cb_crop_circular.isChecked())// 是否圆形裁剪
//                .showCropFrame(true)// 是否显示裁剪矩形边框 圆形裁剪时建议设为false
//                .showCropGrid(true)// 是否显示裁剪矩形网格 圆形裁剪时建议设为false
                .openClickSound(true)// 是否开启点击声音
                .selectionMedia(selectList)// 是否传入已选图片
//                .isDragFrame(false)// 是否可拖动裁剪框(固定)
//                        .videoMaxSecond(15)
//                        .videoMinSecond(10)
                //.previewEggs(false)// 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中)
                .cropCompressQuality(100)// 裁剪压缩质量 默认100
                .minimumCompressSize(500)// 小于100kb的图片不压缩
                .cropWH(1, 1)// 裁剪宽高比，设置如果大于图片本身宽高则无效
//                .rotateEnabled(true) // 裁剪是否可旋转图片
//                .scaleEnabled(true)// 裁剪是否可放大缩小图片
                //.videoQuality()// 视频录制质量 0 or 1
                //.videoSecond()//显示多少秒以内的视频or音频也可适用
                //.recordVideoSecond()//录制视频秒数 默认60s
                .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调onActivityResult code

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    searchDevice();
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(MainActivity.this, "拒绝开启蓝牙将无法使用打印功能", Toast.LENGTH_SHORT).show();
                }
                break;
            case Constant.REQUEST_QR_CODE:
//                String methodName = "getResult" + JSNativeInterface.type;
//                String methodName = "QR_handler";
                if (resultCode == RESULT_OK && data != null) {
                    String result = data.getStringExtra("result");
//                    mAgentWeb.getJsAccessEntrace().quickCallJs(methodName, result);
//                } else {
//                    mAgentWeb.getJsAccessEntrace().quickCallJs(methodName, "-1");
                    JSNativeInterface.isOver = false;
                    JSNativeInterface.resultData = result;
                }else{
                    JSNativeInterface.isOver = false;
                    JSNativeInterface.resultData = "";
                }
                break;
            case PictureConfig.CHOOSE_REQUEST:
                if (resultCode != RESULT_OK) {
                    if (mUploadMessage != null) {
                        mUploadMessage.onReceiveValue(null);
                    }
                    if (mFilePathCallback != null)
                        mFilePathCallback.onReceiveValue(null);
                    return;
                }
                if (null == mUploadMessage && null == mFilePathCallback) {
                    mUploadMessage.onReceiveValue(null);
                    return;
                }
                selectList = PictureSelector.obtainMultipleResult(data);
                Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
                if (mFilePathCallback != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        onActivityResultAboveL(requestCode, resultCode, data);
                    }
                } else if (mUploadMessage != null) {
                    if (result != null) {
                        LocalMedia localMedia = selectList.get(0);
                        String path = null;
                        if (localMedia.isCut() && !localMedia.isCompressed()) {
                            path = localMedia.getCutPath();// 裁剪过
                        } else if (localMedia.isCompressed() || (localMedia.isCut() && localMedia.isCompressed())) {
                            path = localMedia.getCompressPath();// 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                        } else {
                            path = localMedia.getPath(); // 原图
                        }
                        Uri uri = ToolsUtils.getImageContentUri(this, new File(path));
                        mUploadMessage.onReceiveValue(uri);
                    } else {
                        mUploadMessage.onReceiveValue(null);
                    }
                    mUploadMessage = null;
                }
                break;
        }
    }


    @SuppressWarnings("null")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent data) {
        if (requestCode != PictureConfig.CHOOSE_REQUEST
                || mFilePathCallback == null) {
            return;
        }

        Uri[] results = null;
        if (data == null) {
            results = null;
            Log.e(TAG, "results" + results.toString());
        } else {
            selectList = PictureSelector.obtainMultipleResult(data);
            if (selectList != null && selectList.size() > 0) {
                results = new Uri[selectList.size()];
                String path = "";
                for (int i = 0; i < selectList.size(); i++) {
                    LocalMedia localMedia = selectList.get(i);
                    if (localMedia.isCut() && !localMedia.isCompressed()) {
//                    // 裁剪过
                        path = localMedia.getCutPath();
                    } else if (localMedia.isCompressed() || (localMedia.isCut() && localMedia.isCompressed())) {
                        // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                        path = localMedia.getCompressPath();
                    } else {
                        // 原图
                        path = localMedia.getPath();
                    }
                    Uri uri = ToolsUtils.getImageContentUri(this, new File(path));
                    results[i] = uri;
                }
            }
            String dataString = data.getDataString();
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                results = new Uri[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    results[i] = item.getUri();
                }
            }
        }
        if (results != null) {
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        } else {
            results = null;
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }

        return;
    }


    @Nullable
    @Override
    protected WebChromeClient getWebChromeClient() {
        return new WebChromeClient() {
            // For Android 5.0+
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                mFilePathCallback = filePathCallback;
                open();
                return true;
            }

            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                open();
            }

            //3.0--版本
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                open();
            }

            // For Android 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUploadMessage = uploadMsg;
                open();
            }


        };
    }

    @Nullable
    @Override
    protected WebViewClient getWebViewClient() {
        return new WebViewClient() {

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                shouldOverrideUrlLoading(view, request.getUrl().toString());
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if (uri.getHost().equals("startQRCode")) {
                    String arg1 = uri.getQueryParameter("arg1");
                    String arg2 = uri.getQueryParameter("arg2");
                    Log.v(TAG, arg1);
                    Log.v(TAG, arg2);
                    searchBlueToothDevice(arg1 + arg2);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        };
    }

    @Nullable
    @Override
    protected PermissionInterceptor getPermissionInterceptor() {
        return mPermissionInterceptor;
    }

    protected PermissionInterceptor mPermissionInterceptor = new PermissionInterceptor() {

        @Override
        public boolean intercept(String url, String[] permissions, String action) {
            Log.i(TAG, "url:" + url + "  permission:" + permissions + " action:" + action);
            return false;
        }
    };


    public void searchBlueToothDevice(String data) {
        this.data = data;
        Log.i(TAG, "searchBlueToothDevice(MainActivity.java:112)--->> " + "searchBlueToothDevice");

        mBluetoothList = new ArrayList<>();
        // 检查设备是否支持蓝牙
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(this, "当前设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        // 如果蓝牙已经关闭就打开蓝牙
        if (!adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(intent, Constant.REQUEST_ENABLE_BT);
        } else if (adapter.isEnabled()) {
            searchDevice();
        }


//        // 获取已配对的蓝牙设备
//        Set<BluetoothDevice> devices = adapter.getBondedDevices();
//        // 遍历
//        int count = 0;
//        for (BluetoothDevice pairedDevice : devices) {
//            Log.i(TAG, "searchBlueToothDevice(MainActivity.java:137)--->> " + pairedDevice.getName());
//            if (pairedDevice.getName() == null) {
//                return;
//            } else if (pairedDevice.getName().startsWith("Printer_29D0")) {
//                count++;
//                deviceAddress = pairedDevice.getAddress();
//                mBluetoothDevice = adapter.getRemoteDevice(deviceAddress);
//                connect(deviceAddress, mBluetoothDevice);
//                break;
//            }
//        }

    }

    void searchDevice() {
        try {
            if (socket != null && socket.isConnected()) {
                if (outputStream == null) {
                    outputStream = socket.getOutputStream();
                }
                printOrder();
                return;
            } else if (socket != null && !socket.isConnected()) {
                socket.connect();
                outputStream = socket.getOutputStream();
                printOrder();
                return;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (adapter.isEnabled()) {
            //等待弹窗
            pdSearch = ProgressDialog.show(MainActivity.this, "", "搜索设备中", true, true);
            pdSearch.setCanceledOnTouchOutside(false);
            pdSearch.show();

            //开始搜索
            adapter.startDiscovery();
            // 设置广播信息过滤
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            // 注册广播接收器，接收并处理搜索结果
            receiver = new MyBroadcastReceiver();
            registerReceiver(receiver, intentFilter);
        }
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //找到设备,有可能重复搜索同一设备,可在结束后做去重操作
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) {
                    return;
                }
                if (device.getName() == null) {
                    return;
                }

                BluetoothBean bluetoothBean = new BluetoothBean();
                bluetoothBean.mBluetoothName = device.getName();
                bluetoothBean.mBluetoothAddress = device.getAddress();
                bluetoothBean.mBluetoothDevice = adapter.getRemoteDevice(bluetoothBean.mBluetoothAddress);
                mBluetoothList.add(bluetoothBean);

                Log.i(TAG, "onReceive(MainActivity.java:184)--->> " + device.getName());
                Log.i(TAG, "onReceive(MainActivity.java:185)--->> " + mBluetoothList.size());

//                if (device.getName().startsWith("Printer_29D0")) {
//                    //取消搜索
//                    adapter.cancelDiscovery();
//                    deviceAddress = device.getAddress();
//                    mBluetoothDevice = adapter.getRemoteDevice(deviceAddress);
//                    connectState = device.getBondState();
//                    switch (connectState) {
//                        // 未配对
//                        case BluetoothDevice.BOND_NONE:
//                            // 配对
//                            try {
//                                Method createBondMethod = mBluetoothDevice.getClass().getMethod("createBond");
//                                createBondMethod.invoke(mBluetoothDevice);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                            break;
//                        // 已配对
//                        case BluetoothDevice.BOND_BONDED:
//                            if (device.getName().startsWith("Printer_29D0")) {
//                                connect(deviceAddress, mBluetoothDevice);
//                            }
//                            break;
//                    }
//                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "onReceive(MainActivity.java:213)--->> " + "搜索完成");
                if (pdSearch.isShowing())
                    pdSearch.dismiss();
                if (0 == mBluetoothList.size())
                    Toast.makeText(MainActivity.this, "搜索不到蓝牙设备", Toast.LENGTH_SHORT).show();
                else {
                    //去重HashSet add会返回一个boolean值，插入的值已经存在就会返回false 所以true就是不重复的
                    HashSet<BluetoothBean> set = new HashSet<>();
                    mBluetoothList2 = new ArrayList<>();
                    for (BluetoothBean bean : mBluetoothList) {
                        boolean add = set.add(bean);
                        if (add) {
                            mBluetoothList2.add(bean);
                        }
                    }
                    showBluetoothPop(mBluetoothList2);
                }

                unregisterReceiver(receiver);
            }
        }
    }

    private void showBluetoothPop(final List<BluetoothBean> bluetoothList) {
        if (pdSearch.isShowing())
            pdSearch.dismiss();
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_bluetooth, null);
        ListView mListView = view.findViewById(R.id.lv_bluetooth);
        MyBluetoothAdapter myBluetoothAdapter = new MyBluetoothAdapter();
        mListView.setAdapter(myBluetoothAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (0 != mBluetoothList2.size()) {
                    closePopupWindow();
                    pdConnect = ProgressDialog.show(MainActivity.this, "", "开始连接", true, true);
                    pdConnect.setCanceledOnTouchOutside(false);
                    pdConnect.show();
                    connect(bluetoothList.get(position).mBluetoothAddress, bluetoothList.get(position).mBluetoothDevice);

                    Log.v(TAG, bluetoothList.get(position).mBluetoothAddress + "：前址后数字：" + bluetoothList.get(position).mBluetoothDevice);
                }
            }
        });
        pw = new PopupWindow(view, (int) (getScreenWidth() * 0.8), -2);
        closePopupWindow();
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setOutsideTouchable(true);
        pw.setFocusable(true);
        WindowManager.LayoutParams lp = this.getWindow().getAttributes();
        lp.alpha = 0.7f;
        getWindow().setAttributes(lp);
        pw.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = MainActivity.this.getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });
        pw.setAnimationStyle(R.style.PopAnim);
        //显示
        pw.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    private void closePopupWindow() {
        if (pw != null && pw.isShowing()) {
            pw.dismiss();
            pw = null;
        }
    }

    public int getScreenWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    class MyBluetoothAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mBluetoothList2.size();
        }

        @Override
        public Object getItem(int position) {
            return mBluetoothList2.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_bluetooth, parent, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.item_text = convertView.findViewById(R.id.item_text);
            holder.item_text_address = convertView.findViewById(R.id.item_text_address);
            holder.item_type = convertView.findViewById(R.id.item_type);
            holder.item_text.setText(mBluetoothList2.get(position).mBluetoothName);
            holder.item_text_address.setText(mBluetoothList2.get(position).mBluetoothAddress);
            return convertView;
        }

        class ViewHolder {
            TextView item_text;
            TextView item_text_address;
            TextView item_type;
        }
    }


    private void showSuccessDialog() {
        pdSearch.dismiss();
        DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case Dialog.BUTTON_POSITIVE:
                        printOrder();
                        break;
                    case Dialog.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("提示");
        builder.setMessage("连接成功，是否开始打印?");
        builder.setPositiveButton("确定", mOnClickListener);
        builder.setNegativeButton("取消", mOnClickListener);
        builder.create().show();
    }

    private void showErrorDialog() {
        pdSearch.dismiss();
        DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case Dialog.BUTTON_POSITIVE:
                        searchBlueToothDevice(data);
                        break;
                    case Dialog.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("提示");
        builder.setMessage("连接失败，是否重试?");
        builder.setPositiveButton("确定", mOnClickListener);
        builder.setNegativeButton("取消", mOnClickListener);
        builder.create().show();
    }


    /**
     * 启动连接蓝牙的线程方法
     */
    public synchronized void connect(String macAddress, BluetoothDevice device) {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        if (socket != null) {
            socket = null;
        }
        mThread = new ConnectThread(macAddress, device);
        mThread.start();
    }

    private class ConnectThread extends Thread {
        private BluetoothDevice mmDevice;
        private OutputStream mmOutStream;

        public ConnectThread(String mac, BluetoothDevice device) {
            mmDevice = device;
            try {
                if (socket == null) {
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            adapter.cancelDiscovery();
            try {
                Log.i(TAG, "run(MainActivity.java:367)--->> " + "连接socket");
                if (socket.isConnected()) {
                    Log.i(TAG, "run(MainActivity.java:369)--->> " + "已经连接过了");
                } else {
                    if (socket != null) {
                        try {
                            Method createBond = BluetoothDevice.class.getMethod("createBond");
                            createBond.invoke(mmDevice);
                            registerBroadcast();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception connectException) {
                Log.i(TAG, "run(MainActivity.java:402)--->> " + "连接失败");
                try {
                    if (socket != null) {
                        socket = null;
                    }
                } catch (Exception closeException) {

                }
            }
        }
    }

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(printerStatusBroadcastReceiver, filter);
    }

    private BroadcastReceiver printerStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_NONE:
                        Log.e(TAG, "取消配对");
                        unregisterReceiver(printerStatusBroadcastReceiver);
                        if (pdConnect.isShowing())
                            pdConnect.dismiss();
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Log.e(TAG, "配对中");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.e(TAG, "配对成功");
                        unregisterReceiver(printerStatusBroadcastReceiver);
                        if (pdConnect.isShowing())
                            pdConnect.dismiss();
                        if (connect(device)) {
                            showSuccessDialog();
                        }
                        break;
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 开始打印
     */
    private void printOrder() {
        Log.v(TAG, data);
        if (data == null) {
            data = "";
        }
        try {
            byte[] sendData = data.getBytes("gbk");
            outputStream.write(sendData, 0, sendData.length);
            outputStream.flush();
        } catch (IOException e) {
            Toast.makeText(this, "发送失败！", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    OutputStream outputStream;

    /**
     * 连接蓝牙设备
     */
    public boolean connect(BluetoothDevice device) {
        try {
            if (!socket.isConnected()) {

                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
                socket.connect();
                outputStream = socket.getOutputStream();
                return true;
            } else {
                if (outputStream == null)
                    outputStream = socket.getOutputStream();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

}
