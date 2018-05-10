package com.nongrj.els.ui.interfaces;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;

import com.nongrj.els.main.app.Constant;

import io.github.xudaojie.qrcodelib.CaptureActivity;

/**
 * Created by FollowMountains on 2018/4/18.
 */

public class JSNativeInterface {

    private Activity context;
    //    private PhotoPopWindow mPhontoPopWindow;
//    private MaterialDialog mPermissionDialog;
//    private TextView mTitleTextView;
//    public static String type = "";

    private String TAG = "FolloMountains_els";

    public JSNativeInterface(Context context) {
        this.context = (Activity) context;
//        this.mPhontoPopWindow = mPhontoPopWindow;
//        this.mPermissionDialog = mPermissionDialog;
//        this.mTitleTextView = mTitleTextView;
    }

//    @JavascriptInterface
//    public void selectImage() {
//
//        //申请必要权限
//        PermissionManager permissionManager = new PermissionManager();
//        permissionManager.requestEachCombined(context, new PermissionListener() {
//            @Override
//            public void onGranted(String permissionName) {
//                //全部权限都被授予的话，则弹出底部选项
//                mPhontoPopWindow.showAtLocation(context.getWindow().getDecorView().findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0);
//            }
//
//            @Override
//            public void onDenied(String permissionName) {
//                //如果用户拒绝了其中一个授权请求，则提醒用户
//                Toast.makeText(context, context.getString(R.string.permission_request), Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onDeniedWithNeverAsk(String permissionName) {
//                //如果用户拒绝了其中一个授权请求，且勾选了不再提醒，则需要引导用户到权限管理页面开启
//                mPermissionDialog.show();
//            }
//        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA);
//    }

    @JavascriptInterface
    public void showCamera() {
//        this.type = type;
        Intent i = new Intent(context, CaptureActivity.class);
        context.startActivityForResult(i, Constant.REQUEST_QR_CODE);
    }

    @JavascriptInterface
    public void setTitle(String title) {
//        mTitleTextView.setText(title);
    }
}
