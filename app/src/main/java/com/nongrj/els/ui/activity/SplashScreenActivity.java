package com.nongrj.els.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;

import com.nongrj.els.R;
import com.nongrj.els.main.activitys.BaseActivity;


/**
 * Created by ZhaoMing on 2016-8-1.
 */
public class SplashScreenActivity extends BaseActivity {

    public static final String ACTION_ADD_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
                finish();

            }
        }, 3000); // 启动动画持续3秒钟
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }
}
