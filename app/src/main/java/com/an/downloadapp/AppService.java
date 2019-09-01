package com.an.downloadapp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.DownloadProgressCallBack;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import androidx.annotation.NonNull;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class AppService extends Service {
    public static String TAG = "AppService";

    private String serverVersion;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG,"onCreate");
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //httpGetVersion();
        Log.e(TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 执行shell命令
     *
     * @param cmd
     */
    private void execShellCmd(String cmd) {

        try {
            // 申请获取root权限，这一步很重要，不然会没有作用
            Process process = Runtime.getRuntime().exec("su");
            // 获取输出流
            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            dataOutputStream.writeBytes(cmd);
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusEvent event) {
        if (event == null) {
            return;
        }
        // 中控过期了
        if (PreferencesUtils.getInstance().getBoolean("app_maturity")){
            Toast.makeText(getApplication(),"您的命令已过期，请联系系统管理员",Toast.LENGTH_LONG).show();
            return;
        }

        switch (event.getMsg()) {
            case "update":
                Log.e(TAG,"service update");
                httpGetVersion();
                break;
            case "restart_touch_sprite":
                Intent intent = new Intent(Intent.ACTION_MAIN);
                //前提：知道要跳转应用的包名、类名
                ComponentName componentName = new ComponentName("com.touchsprite.android", "com.touchsprite.android.activity.MainActivity");
                intent.setComponent(componentName);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                execShellCmd("getevent -p");
                execShellCmd("input tap  662 1137 ");//点击 500,100
                execShellCmd("input tap  662 1137 ");//点击 500,100
                execShellCmd("input tap  662 1137 ");//点击 500,100

                //延时5S 发送
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Message msg = new Message();
                        msg.what = 0; // 0 截图
                        mHandler.sendMessage(msg);
                    }
                },5000);

                break;
            case "app_maturity": //脚本启停时间到期
                PreferencesUtils.getInstance().putBoolean("app_maturity",true);
                break;
            case "app_recharge": //重新充值,购买
                PreferencesUtils.getInstance().putBoolean("app_maturity",false);
                break;
            case "snap_shot": //获取屏幕截图
                execShellCmd("screencap -p /sdcard/snap_shot.png");
                break;
            case "stop_script":
                execShellCmd("screencap -p /sdcard/16.png");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Message msg = new Message();
                        msg.what = 3; // 0 截图
                        mHandler.sendMessage(msg);
                    }
                },7000);
                break;
        }

        // 判断  是否更新某应用  部分更新  part-xxxx,包名
        String content = event.getMsg();
        if (content.contains("part")){
            String[] str =  content.split("-");
            Log.e(TAG,"str[0]"+str[0]);
            Log.e(TAG,"str[1]"+str[1]);
            if (Utils.isAppInDevice(getApplicationContext(),str[1])){
                Log.e(TAG,"手机里有应用 - 更新脚本");
                httpGetVersion();
            }
        }


    }


    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    execShellCmd("screencap -p /sdcard/15.png");
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = new Message();
                            msg.what = 1; // 0 截图
                            mHandler.sendMessage(msg);
                        }
                    },7000);
                    break;

                case 1: //重启触动的服务

                    File param = new File("/sdcard/15.png");
                    Bitmap bitmap= BitmapFactory.decodeFile("/storage/emulated/0/15.png");
                    if (bitmap != null){
                        int color = bitmap.getPixel(705,416);
                        if ("02bfe7".equals(Integer.toHexString(color).substring(2,8))){ //服务是开启的，先关闭
                            execShellCmd("getevent -p");
                            execShellCmd("input tap 705 416 ");//点击 500,100

//                            execShellCmd("input tap  662 1137 ");//点击 500,100  瞎点  拖延时间
//                            execShellCmd("input tap  662 1137 ");//点击 500,100
//                            execShellCmd("input tap  662 1137 ");//点击 500,100

                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            execShellCmd("input tap 705 416 ");// 重新开启

                        }else{ //按钮是灰色,服务死掉了,直接开启
                            execShellCmd("getevent -p");
                            execShellCmd("input tap 705 416 ");//点击 500,100
                        }

                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Message msg = new Message();
                                msg.what = 2; // 0 截图
                                mHandler.sendMessage(msg);
                            }
                        },5000);

                        bitmap.recycle();
                    }


                    break;
                case 2: // 脚本悬浮窗启停
                    File file = new File("/sdcard/15.png");
                    Bitmap bitmap2= BitmapFactory.decodeFile("/storage/emulated/0/15.png");
                    if (bitmap2 != null){
                        int color2 = bitmap2.getPixel(25,1161);
                        if ("00a8e9".equals(Integer.toHexString(color2).substring(2,8))) { // 脚本停止

                            if ("0x00a7ea".equals(Integer.toHexString(color2).substring(2,8))){  //触动悬浮窗点开了
                                execShellCmd("getevent -p");
                                execShellCmd("input tap 65 1161 ");//点击 开始脚本
                            }else {
                                execShellCmd("getevent -p");
                                execShellCmd("input tap 25 1161 ");//点击 悬浮窗
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                execShellCmd("input tap 65 1161 ");//点击 开始脚本
                            }
                            bitmap2.recycle();
                        }else { //服务未开启  或者脚本运行中
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    EventBus.getDefault().post(new EventBusEvent("restart_touch_sprite")); //重启触动服务
                                }
                            },5000);
                        }
                    }

                    break;

                case 3: // 脚本停止的操作
                    Log.e(TAG,"脚本停止的操作");
                    File snap_shot = new File("/sdcard/16.png");
                    Log.e(TAG,"snap_shot ="+snap_shot.exists());
                    Bitmap bitmap_snap= BitmapFactory.decodeFile("/storage/emulated/0/16.png");
                    Log.e(TAG,"bitmap_snap ="+bitmap_snap);
                    if (bitmap_snap != null){
                        int color4 = bitmap_snap.getPixel(25,  777);
                        Log.e(TAG,"脚本停止的操作"+Integer.toHexString(color4).substring(2,8));
                        if ("4ea800".equals(Integer.toHexString(color4).substring(2,8))) { //脚本是运行着的，先关闭
                            execShellCmd("getevent -p");
                            execShellCmd("input tap  25  777");//点击 停止脚本
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            execShellCmd("input tap  109  777");//点击 停止脚本
                            execShellCmd("input tap  109  777");//点击 停止脚本
                        }

                        bitmap_snap.recycle();
                    }else {
                        Log.e(TAG,"bitmap_snap 为空");
                    }

                    break;
            }
        }
    };



    /**
     * 获取服务器版本号
     */
    private void httpGetVersion(){
        EasyHttp
                .get(UrlConstant.GET_VERSION)
                .baseUrl(UrlConstant.BASE_URL)
                .params("version_name","qiji_mu_version")
                .execute(new SimpleCallBack<String>() {
                    @Override
                    public void onError(ApiException e) {
                        Log.e("Main","e="+e.getMessage());
                        Toast.makeText(getApplication(),"获取版本号失败",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(String s) {
                        Log.e("Main","s="+s);
                        serverVersion = s;
                        Toast.makeText(getApplication(),"获取版本号成功",Toast.LENGTH_LONG).show();
                        if ( TextUtils.isEmpty(PreferencesUtils.getInstance().getString("qiji_mu_version")) ||
                                Integer.valueOf(serverVersion) > Integer.valueOf(PreferencesUtils.getInstance().getString("qiji_mu_version"))){
                            downloadFile();
                        }else{
                            Toast.makeText(getApplication(),"当前已经是最新版本",Toast.LENGTH_LONG).show();
                        }
                    }

                });

    }


    /**
     *  下载更新
     */
    private void downloadFile() {
        EasyHttp.downLoad(UrlConstant.DOWNLOAD_FILE2)
                .baseUrl(UrlConstant.BASE_URL)
                //.params("file_name","qiji_mu.zip")
                //.params("file_name","main.lua")
                .savePath("/sdcard/TouchSprite")
                .saveName("qiji_mu.zip")//不设置默认名字是时间戳生成的
                .execute(new DownloadProgressCallBack<String>() {
                    @Override
                    public void update(long bytesRead, long contentLength, boolean done) {
                        int progress = (int) (bytesRead * 100 / contentLength);
                        Log.e("Main",progress + "% ");
                        Log.e("Main-version",PreferencesUtils.getInstance().getString("qiji_mu_version"));
                        //dialog.setProgress(progress);
                        if (done) {//下载完成
                            PreferencesUtils.getInstance().putString("qiji_mu_version",serverVersion);

                            File zipFile = new File(Utils.getSDPath()+"/TouchSprite/qiji_mu.zip");
                            Utils.deleteDirectory(Utils.getSDPath()+"/TouchSprite/lua");
                            String folderPath =  Utils.getSDPath()+"/TouchSprite/";
                            try {
                                Utils.upZipFile(zipFile,folderPath);

                                Utils.deleteFile(Utils.getSDPath()+"/TouchSprite/qiji_mu.zip");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(getApplication(),"下载成功",Toast.LENGTH_LONG).show();
                        }

                    }

                    @Override
                    public void onStart() {
                        //开始下载
                        Log.e("Main","onStart");

                    }

                    @Override
                    public void onComplete(String path) {
                        //下载完成，path：下载文件保存的完整路径
                        Log.e("Main","onComplete");

                    }

                    @Override
                    public void onError(ApiException e) {
                        //下载失败
                        Log.e("Main","onError="+e.getMessage());
                        Log.e("Main","onError="+e.getCode());
                        Toast.makeText(getApplication(),"下载失败",Toast.LENGTH_LONG).show();

                    }
                });

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG,"onDestroy");
        EventBus.getDefault().unregister(this);
        mHandler.removeCallbacksAndMessages(null);
    }
}
