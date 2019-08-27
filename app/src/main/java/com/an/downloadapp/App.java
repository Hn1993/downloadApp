package com.an.downloadapp;

import android.app.Application;
import com.zhouyou.http.EasyHttp;

import cn.jpush.android.api.JPushInterface;

public class App extends Application {

    private static App instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        EasyHttp.init(this);//默认初始化
        JPushInterface.setDebugMode(true); 	// 设置开启日志,发布时请关闭日志
        JPushInterface.init(this);     		// 初始化 JPush

    }

    public static App getInstance() {
        return instance;
    }
}
