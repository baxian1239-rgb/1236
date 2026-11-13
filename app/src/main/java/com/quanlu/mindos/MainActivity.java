package com.quanlu.mindos;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.Toast;
import android.print.PrintManager;
import android.content.Context;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int CHOOSE_FILE = 1101, SAVE_FILE = 1102, REQUEST_NOTIFICATIONS = 1103;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingBytes;
    private boolean pendingEnableReminders;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8,11,24));
        getWindow().setNavigationBarColor(Color.rgb(8,11,24));
        getWindow().getDecorView().setSystemUiVisibility(0);
        webView=new WebView(this);
        webView.setBackgroundColor(Color.rgb(8,11,24));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.addJavascriptInterface(new AndroidBridge(),"AndroidApp");
        webView.setWebViewClient(new WebViewClient(){
            private boolean handleUrl(String url){
                if(url.startsWith("file:///android_asset/"))return false;
                if(url.startsWith("https://") || url.startsWith("http://")){
                    try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}
                }
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return handleUrl(url);}
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req){return handleUrl(req.getUrl().toString());}
            @Override public void onPageFinished(WebView view,String url){ sendReminderStatus(); }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> callback,FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);
                fileCallback=callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                try{startActivityForResult(intent,CHOOSE_FILE);}catch(Exception e){fileCallback=null;callback.onReceiveValue(null);return false;}
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }
    private void notifyText(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private void sendReminderStatus(){
        boolean enabled=ReminderScheduler.enabled(this);
        String message=enabled?"系统提醒已开启；受省电策略影响，通知时间可能略有偏差。":"系统提醒未开启。点击开启后授权通知权限。";
        webView.evaluateJavascript("window.onAndroidReminderStatus && window.onAndroidReminderStatus("+enabled+",'"+message+"');",null);
    }
    private class AndroidBridge {
        @JavascriptInterface public void requestReminderStatus(){runOnUiThread(()->sendReminderStatus());}
        @JavascriptInterface public void setReminders(boolean enabled){runOnUiThread(()->{
            if(enabled && Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
                pendingEnableReminders=true;
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQUEST_NOTIFICATIONS);
            }else{ReminderScheduler.setEnabled(MainActivity.this,enabled);sendReminderStatus();notifyText(enabled?"已设置六次日间提醒":"已关闭提醒");}
        });}
        @JavascriptInterface public void saveFile(String filename,String mime,String base64Data){runOnUiThread(()->{
            try{
                if(base64Data.length()>10_000_000)throw new Exception("文件超过大小限制");
                pendingBytes=Base64.decode(base64Data,Base64.DEFAULT);
                String name=filename.replaceAll("[\\/:*?\"<>|]","_");
                String outMime=(mime.equals("application/json")||mime.equals("image/png"))?mime:"application/octet-stream";
                Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(outMime);
                intent.putExtra(Intent.EXTRA_TITLE,name);
                startActivityForResult(intent,SAVE_FILE);
            }catch(Exception e){notifyText("保存失败："+e.getMessage());}
        });}
        @JavascriptInterface public void printPage(){runOnUiThread(()->{
            PrintManager manager=(PrintManager)getSystemService(Context.PRINT_SERVICE);
            if(manager!=null)manager.print("心智成长OS打印",webView.createPrintDocumentAdapter("心智成长OS"),null);
        });}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==REQUEST_NOTIFICATIONS){
            if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED&&pendingEnableReminders){
                ReminderScheduler.setEnabled(this,true);
                notifyText("已设置日间提醒");
            }else notifyText("通知权限未授予，无法显示提醒");
            pendingEnableReminders=false;sendReminderStatus();
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==CHOOSE_FILE){
            if(fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null);fileCallback=null;}
        }else if(request==SAVE_FILE){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null&&pendingBytes!=null){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    if(out!=null){out.write(pendingBytes);notifyText("文件已保存");}
                }catch(Exception e){notifyText("写入失败："+e.getMessage());}
            }
            pendingBytes=null;
        }
    }
    @Override public void onBackPressed(){
        if(webView!=null&&webView.canGoBack())webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){if(webView!=null){webView.destroy();webView=null;}super.onDestroy();}
}
