package com.an.downloadapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AppReceiver extends BroadcastReceiver {
    public String TAG = "AppReceiver";
    /*要接收的intent源*/
    static final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";
    static final String ACTION_TIME_TICK = "android.intent.action.TIME_TICK";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG,"action="+intent.getAction());
        if (ACTION_BOOT.equals(intent.getAction())){ //开机启动 服务
            Intent intent1 = new Intent(context,AppService.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(intent1);
        }else if (ACTION_TIME_TICK.equals(intent.getAction())){ //时间服务
            if (!Utils.isServiceExisted(context,"com.an.downloadapp.AppService")){
                //启动Service
                Intent intent2 = new Intent(context, AppService.class);
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startService(intent2);
            }
        }
    }
}
