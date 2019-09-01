package com.an.downloadapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.CallBack;
import com.zhouyou.http.callback.DownloadProgressCallBack;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;
import com.zhouyou.http.utils.HttpLog;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import retrofit2.http.Url;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView tvLocalVersion;
    private TextView tvServerVersion;
    private Button btDownload;
    private Button btRefresh;
    private Button tap;
    private String serverVersion = "1";

    private ProgressDialog dialog;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e("TAG","version="+Utils.getVersionCode(this));
        tvLocalVersion = findViewById(R.id.version_local);
        tvServerVersion = findViewById(R.id.version_server);
        btDownload = findViewById(R.id.server_download);
        btRefresh = findViewById(R.id.server_refresh);
        tap = findViewById(R.id.tap);
        btDownload.setOnClickListener(this);
        btRefresh.setOnClickListener(this);
        tap.setOnClickListener(this);

        boolean flag = Utils.isAppInDevice(this,"com.tencent.yoozoo.got.wintercoming");
        Log.e("main","flag="+flag);

        if (!Utils.isServiceExisted(this,"com.an.downloadapp.AppService")){
            //启动Service
            Intent intent = new Intent(this, AppService.class);
            startService(intent);
        }




        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .permission(Permission.Group.LOCATION)
                .onGranted(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {

                    }
                })
                .start();
        

        
        init();
        //checkUpdate();


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


    /**
     * 检查更新
     */
    private void checkUpdate() {
        EasyHttp
                .get(UrlConstant.GET_APP_VERSION)
                .baseUrl(UrlConstant.BASE_URL)
                .params("app_version","app_version")
                .execute(new SimpleCallBack<String>() {
                    @Override
                    public void onError(ApiException e) {
                        Log.e("Main","e="+e.getMessage());
                        Toast.makeText(MainActivity.this,"获取APP版本号失败",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(String s) {
                        Log.e("Main","s="+s);
                        Toast.makeText(MainActivity.this,"获取APP版本号成功",Toast.LENGTH_LONG).show();
                        if( Integer.valueOf(s) >  Utils.getVersionCode(MainActivity.this)){
                            // 弹出对话框 更新app
                            downLoadApp();
                        }
                    }

                });
    }


    private void downLoadApp(){
        Utils.deleteFile("/sdcard/update_app.apk");
        EasyHttp.downLoad(UrlConstant.DOWNLOAD_APP)
                .baseUrl(UrlConstant.BASE_URL)
                //.params("file_name","qiji_mu.zip")
                //.params("file_name","main.lua")
                .savePath("/sdcard")
                .saveName("update_app.apk")//不设置默认名字是时间戳生成的
                .execute(new DownloadProgressCallBack<String>() {

                    @Override
                    public void onStart() {
                        dialog.show();
                    }

                    @Override
                    public void onError(ApiException e) {
                        dialog.dismiss();
                    }

                    @Override
                    public void update(long bytesRead, long contentLength, boolean done) {
                        int progress = (int) (bytesRead * 100 / contentLength);
                        dialog.setProgress(progress);
                        if (done) {
                            dialog.setMessage("下载完成");
                            dialog.dismiss();

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setDataAndType(Uri.fromFile(new File("/sdcard/update_app.apk")), "application/vnd.android.package-archive");
                            startActivity(intent);

                            //boolean flag = installUseRoot("/sdcard/update_app.apk");
                            //boolean flag = installApp("/sdcard/update_app.apk");
                            //boolean flag = slientInstall(new File("/sdcard/update_app.apk"));
                            //Log.e("TAG","flag="+flag);
                            //httpGetVersion();
                            //installRoot("/sdcard/update_app.apk");
                        }
                    }

                    @Override
                    public void onComplete(String path) {
                        dialog.dismiss();
                    }
                });
    }


    /**
     * 静默安装
     * @param file
     * @return
     */
    public boolean slientInstall(File file) {
        boolean result = false;
        Process process = null;
        OutputStream out = null;
        try {
            process = Runtime.getRuntime().exec("su");
            out = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(out);
            dataOutputStream.writeBytes("chmod 777 " + file.getPath() + "\n");
            dataOutputStream.writeBytes("LD_LIBRARY_PATH=/vendor/lib:/system/lib pm install -r " +
                    file.getPath());
            // 提交命令
            dataOutputStream.flush();
            // 关闭流操作
            dataOutputStream.close();
            out.close();
            int value = process.waitFor();

            // 代表成功
            if (value == 0) {
                result = true;
            } else if (value == 1) { // 失败
                result = false;
            } else { // 未知情况
                result = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.e("TAG","result="+result);
        return result;
    }



    public static boolean installApp(String apkPath) {
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        try {
            //process = new ProcessBuilder("pm", "install", "-i", "com.example.ddd", "-r", apkPath).start();
            process = new ProcessBuilder("pm", "install", "-r", apkPath).start();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
        } catch (Exception e) {

        } finally {
            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (Exception e) {

            }
            if (process != null) {
                process.destroy();
            }
        }
        Log.e("result", "" + errorMsg.toString());
        //如果含有“success”单词则认为安装成功
        return successMsg.toString().equalsIgnoreCase("success");
    }

    /**
     *  静默安装apk
     */
    /**
     * 执行具体的静默安装逻辑，需要手机ROOT。
     *
     * @param apkPath
     * @return 安装成功返回true，安装失败返回false。
     */
    public void installRoot(final String apkPath) {
        new AsyncTask<Boolean, Object, Boolean>() {
            @Override
            protected Boolean doInBackground(Boolean... params) {
                boolean result = false;
                DataOutputStream dataOutputStream = null;
                BufferedReader errorStream = null;
                try {
                    // 申请su权限
                    Process process = Runtime.getRuntime().exec("su");
                    dataOutputStream = new DataOutputStream(process.getOutputStream());
                    // 执行pm install命令
                    String command = "pm install -r " + apkPath + "\n";
                    dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));
                    dataOutputStream.flush();
                    dataOutputStream.writeBytes("exit\n");
                    dataOutputStream.flush();
                    int i = process.waitFor();
                    if (i == 0) {
                        result = true; // 正确获取root权限
                    } else {
                        result = false; // 没有root权限，或者拒绝获取root权限
                    }
                } catch (Exception e) {
                    Log.e("TAG", e.getMessage(), e);
                } finally {
                    try {
                        if (dataOutputStream != null) {
                            dataOutputStream.close();
                        }
                        if (errorStream != null) {
                            errorStream.close();
                        }
                    } catch (IOException e) {
                        Log.e("TAG", e.getMessage(), e);
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(Boolean hasRoot) {
                Toast.makeText(MainActivity.this, "hasRoot="+hasRoot, Toast.LENGTH_SHORT).show();
                if (!hasRoot) {
                    //installAuto(apkPath);
                } else {

                }
            }
        }.execute();
    }

    private void init() {
        String local_version = PreferencesUtils.getInstance().getString("qiji_mu_version");
        if (TextUtils.isEmpty(local_version)){
            PreferencesUtils.getInstance().putString("qiji_mu_version","1");
        }

        //httpGetVersion();
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);// 设置进度条的形式为圆形转动的进度条
        dialog.setMessage("正在下载...");
        // 设置提示的title的图标，默认是没有的，如果没有设置title的话只设置Icon是不会显示图标的
        dialog.setTitle("下载文件");
        dialog.setMax(100);
    }


    private void httpGetVersion(){
        EasyHttp
                .get(UrlConstant.GET_VERSION)
                .baseUrl(UrlConstant.BASE_URL)
                .params("version_name","qiji_mu_version")
                .execute(new SimpleCallBack<String>() {
                    @Override
                    public void onError(ApiException e) {
                        Log.e("Main","e="+e.getMessage());
                        Toast.makeText(MainActivity.this,"获取版本号失败",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(String s) {
                        Log.e("Main","s="+s);
                        updateUI((String) s);
                        serverVersion = s;
                        Toast.makeText(MainActivity.this,"获取版本号成功",Toast.LENGTH_LONG).show();
                    }

                });

    }

    private void updateUI(String serverVersion){
        tvLocalVersion.setText("本地版本号: "+PreferencesUtils.getInstance().getString("qiji_mu_version"));
        tvServerVersion.setText("服务器版本号: "+serverVersion);
        //btDownload.setClickable(Integer.valueOf(serverVersion) > Integer.valueOf(PreferencesUtils.getInstance().getString("qiji_mu_version")) ? true : false);

    }

    private void downloadFile(){
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
                            Toast.makeText(MainActivity.this,"下载成功",Toast.LENGTH_LONG).show();
                        }

                        dialog.setProgress(progress);
                        if (done) {
                            dialog.setMessage("下载完成");
                        }

                    }

                    @Override
                    public void onStart() {
                        //开始下载
                        Log.e("Main","onStart");
                        dialog.show();
                    }

                    @Override
                    public void onComplete(String path) {
                        //下载完成，path：下载文件保存的完整路径
                        Log.e("Main","onComplete");
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(ApiException e) {
                        //下载失败
                        Log.e("Main","onError="+e.getMessage());
                        Log.e("Main","onError="+e.getCode());
                        Toast.makeText(MainActivity.this,"下载失败",Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.server_download:
                if ( !TextUtils.isEmpty(PreferencesUtils.getInstance().getString("qiji_mu_version")) &&
                        Integer.valueOf(serverVersion) <= Integer.valueOf(PreferencesUtils.getInstance().getString("qiji_mu_version"))){
                    Toast.makeText(MainActivity.this,"当前已经是最新版本",Toast.LENGTH_LONG).show();
                }else{
                    downloadFile();
                }
                //okGoDownload();
                break;
            case R.id.server_refresh:
                httpGetVersion();
                break;
            case R.id.tap:
//                execShellCmd("getevent -p");
//                execShellCmd("input tap 685 639");//点击 500,100
//                Log.e("TAG","点击成功");
                Log.e("TAG","tap");
                //forceStopProgress(MainActivity.this,"om.touchsprite.android");

//                int pid  = getPidByPkgName(MainActivity.this,"com.touchsprite.android");
//                if (pid != -1 ){
//                    doExec(String.valueOf(pid));
//                }

                Intent intent = new Intent(Intent.ACTION_MAIN);
                //前提：知道要跳转应用的包名、类名
                ComponentName componentName = new ComponentName("com.touchsprite.android", "com.touchsprite.android.activity.MainActivity");
                intent.setComponent(componentName);
                startActivity(intent);


                execShellCmd("getevent -p");
                execShellCmd("input tap  662 1137 ");//点击 500,100
                execShellCmd("input tap  662 1137 ");//点击 500,100


//                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "zhangphil.jpg");
//                boolean ret = save(screenShot(MainActivity.this), file, Bitmap.CompressFormat.JPEG, true);
//                if (ret) {
//                    Toast.makeText(getApplicationContext(), "截图已保持至 " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//                }

                //exec("adb shell /system/bin/screencap -p "+ Environment.getExternalStorageState() + "/11.png");
                execShellCmd("screencap -p /sdcard/13.png");
                execShellCmd("kill -9" + getPidByPkgName(MainActivity.this,"com.touchsprite.android"));
                break;
        }
    }



    private String exec(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            process.waitFor();
            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


        /**
         * 保存图片到文件File。
         *
         * @param src     源图片
         * @param file    要保存到的文件
         * @param format  格式
         * @param recycle 是否回收
         * @return true 成功 false 失败
         */
    public static boolean save(Bitmap src, File file, Bitmap.CompressFormat format, boolean recycle) {

        OutputStream os;
        boolean ret = false;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            ret = src.compress(format, 100, os);
            if (recycle && !src.isRecycled())
                src.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    /**
     * 获取当前屏幕截图，不包含状态栏（Status Bar）。
     *
     * @param activity activity
     * @return Bitmap
     */
    public  Bitmap screenShot(Context activity) {
        View view = getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bmp = view.getDrawingCache();
        int statusBarHeight = getStatusBarHeight(activity);
        int width = (int) getDeviceDisplaySize(activity)[0];
        int height = (int) getDeviceDisplaySize(activity)[1];

        Bitmap ret = Bitmap.createBitmap(bmp, 0, statusBarHeight, width, height - statusBarHeight);
        view.destroyDrawingCache();

        return ret;
    }

    public static float[] getDeviceDisplaySize(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        float[] size = new float[2];
        size[0] = width;
        size[1] = height;

        return size;
    }

    public static int getStatusBarHeight(Context context) {
        int height = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height = context.getResources().getDimensionPixelSize(resourceId);
        }

        return height;
    }

    /**
     * Bitmap对象是否为空。
     */
    public static boolean isEmptyBitmap(Bitmap src) {
        return src == null || src.getWidth() == 0 || src.getHeight() == 0;
    }










    public int  getPidByPkgName(Context context, String pkgName){
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> mRunningProcess = am.getRunningAppProcesses();
        int pid=-1;
        //int i = 1;
        for (ActivityManager.RunningAppProcessInfo amProcess : mRunningProcess){
            if(amProcess.processName.equals(pkgName)){
                pid=amProcess.pid;
                break;
            }
        }
        return pid;
    }

    /**
     * 通过Java的反射机制强制杀死进程
     * @author hanhao
     * com.touchsprite.android
     */
    public void forceStopProgress(Context context, String pkgName) {

        Log.e("TAG","forceStopProgress");
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        try {
            Method forceStopPackage = am.getClass().getDeclaredMethod("forceStopPackage", String.class);
            forceStopPackage.setAccessible(true);
            forceStopPackage.invoke(am, pkgName);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
