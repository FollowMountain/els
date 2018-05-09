package com.nongrj.els.widget;

/**
 * Created by FollowMountains on 2018/4/25.
 */

public interface PermissionListener {
    void onGranted(String permissionName);//成功授权

    void onDenied(String permissionName);//被拒绝

    void onDeniedWithNeverAsk(String permissionName);//被拒绝，且勾选了“不再提示”
}