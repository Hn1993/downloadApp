package com.an.downloadapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.List;

import retrofit2.http.Url;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView tvLocalVersion;
    private TextView tvServerVersion;
    private Button btDownload;
    private Button btRefresh;
    private String serverVersion;

    private ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocalVersion = findViewById(R.id.version_local);
        tvServerVersion = findViewById(R.id.version_server);
        btDownload = findViewById(R.id.server_download);
        btRefresh = findViewById(R.id.server_refresh);
        btDownload.setOnClickListener(this);
        btRefresh.setOnClickListener(this);

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


    }

    private void init() {
        String local_version = PreferencesUtils.getInstance().getString("qiji_mu_version");
        if (TextUtils.isEmpty(local_version)){
            PreferencesUtils.getInstance().putString("qiji_mu_version","1");
        }

        httpGetVersion();

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
                if (Integer.valueOf(serverVersion) <= Integer.valueOf(PreferencesUtils.getInstance().getString("qiji_mu_version"))){
                    Toast.makeText(MainActivity.this,"当前已经是最新版本",Toast.LENGTH_LONG).show();
                }else{
                    downloadFile();
                }
                //okGoDownload();
                break;
            case R.id.server_refresh:
                httpGetVersion();
                break;
        }
    }


}
