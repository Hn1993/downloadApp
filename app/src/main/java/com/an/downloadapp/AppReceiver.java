package com.an.downloadapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppReceiver extends BroadcastReceiver {

    /*要接收的intent源*/
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == ACTION){ //开机启动 服务
            Intent intent1 = new Intent(context,AppService.class);
            context.startService(intent1);
        }
    }
}
