package com.an.downloadapp;

import android.app.Application;
import com.zhouyou.http.EasyHttp;

public class App extends Application {

    private static App instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        EasyHttp.init(this);//默认初始化
    }

    public static App getInstance() {
        return instance;
    }
}
