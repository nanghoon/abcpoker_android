package com.work.abc;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    String tag = "ABC POKER";
    private WebView webView;
    private String url_main = "http://abc-pokertest.co.kr/abcpoker/allPage.do";
    //private String url_main = "http://183.102.237.232:8080/abcpoker/allPage.do";

    public Dialog dialog = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkVerify(); // 권한 체크

        webView = (WebView) findViewById(R.id.main_webview);
        webView.getSettings().setJavaScriptEnabled(true); // js로 이루어져있는 기능들을 사용하기 위해 속성추가
        webView.addJavascriptInterface( this, "android"); // javascript 인터페이스 설정
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true); // 팝업창을 띄울때 속성을 추가해야 window.open이 제대로 작동
        webView.getSettings().setDomStorageEnabled(true); // 로컬스토리지 사용여부 ( ex 팝업 하루동안 보지않기 )
        webView.getSettings().setBuiltInZoomControls(false); // 확대/축소 가능여부
        webView.setInitialScale(100); // 페이지 기본 확대/축소 설정
        webView.getSettings().setUseWideViewPort(true); // 메타태그지원활성화 or 넓은 뷰포트 사용 설정 (false인 경우 webview의 컨트롤 너비로 설정)
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.getSettings().setSupportMultipleWindows(true); // 카카오톡 설정  다중 윈도우 허용(팝업을 위해 추가)
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 화면 세로고정
        getWindow().getDecorView().setSystemUiVisibility(
                webView.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | webView.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | webView.SYSTEM_UI_FLAG_IMMERSIVE
                        | webView.SYSTEM_UI_FLAG_FULLSCREEN
                        | webView.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        // 결제
        mainContext = this;
        billingManager = new BillingManager(MainActivity.this);
        // -- 결제

        // 배터리 최적화 설정 ( 화면 꺼져도 작동하도록 )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
        // --

        Timer timer = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        //소스
                        //Log.d(tag , "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
                        getWindow().getDecorView().setSystemUiVisibility(
                                webView.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        | webView.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | webView.SYSTEM_UI_FLAG_IMMERSIVE
                                        | webView.SYSTEM_UI_FLAG_FULLSCREEN
                                        | webView.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        );
                    }
                });
            }

        };

        timer.schedule(tt, 0 , 3000);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) { // 페이지 컨트롤을 위한 기본적인 함수
                Log.d(tag, "url =================================:" + url);
                if (url.startsWith("intent:")) { // 카카오톡 설정
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                        if (existPackage != null) {
                            startActivity(intent);
                        } else {
                            Log.d(tag, "Could not parse anythings");
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage())); // 카카오톡 마켓으로 이동
                            startActivity(marketIntent);
                        }

                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    view.loadUrl(url);
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() { // js alert 띄우기 위해 추가해야함

            // window.open 시 호출되는 함수
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(view.getContext());
                WebSettings settings = newWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                settings.setSupportMultipleWindows(true);

                // 웹뷰를 띄워줄 다이얼로그
                dialog = new Dialog(view.getContext(), R.style.Theme_Abc);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(newWebView);
                dialog.show();
                dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    // new webView 백버튼
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if(keyCode == KeyEvent.KEYCODE_BACK) {
                            if(newWebView.canGoBack()){
                                Log.d(tag , "onKey canGoBack.......");
                                newWebView.goBack();
                            }else{
                                Log.d(tag , "onKey else.......");
                                newWebView.setVisibility(View.GONE);
                                newWebView.destroy();
                                dialog.dismiss();
                            }
                            return true;
                        }else{
                            return false;
                        }
                    }
                });
                newWebView.setWebViewClient(new WebViewClient());
                newWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        Log.d(tag , "onCloseWindow .......");
                        dialog.dismiss();
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();

                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                Log.d(tag , "Call onCloseWindow...");
                window.setVisibility(View.GONE);
                window.destroy();
                super.onCloseWindow(window);
            }
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            // For Android 5.0+
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                if (filePathCallbackLollipop != null) {
                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                }
                filePathCallbackLollipop = filePathCallback;
                checkVerify();
                readFile();
                return true;
            }
        });
        webView.loadUrl(url_main); // 셋팅된 url 을 불러오는 함수

    }

    @JavascriptInterface
    public void appRestart(){
        Log.d(tag , "Call Restart...");
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        startActivity(mainIntent);
        System.exit(0);
    }

    // 로그인 정보 저장 관련 =====

    private Context context; // 캐시

    @JavascriptInterface
    public int loginStat(){
        Log.d(tag , "loginStat");
        try{
            FileInputStream fis = openFileInput("loginCache");
            BufferedReader buffer = new BufferedReader(new InputStreamReader(fis));
            String str = buffer.readLine();
            return 1;
        }catch (Exception e) {
            return 0;
        }
    }
    public static int saveUserIdx;
    @JavascriptInterface
    public void loginSave(int useridx , String id , String pw){
        Log.d(tag, "function loginSave useridx : "+useridx + " id : " + id + " pw : " + pw + " id equals null " + (id.equals("null")));
        if(useridx != 0 && !id.equals("null")){
            FileOutputStream outputStream;
            String fileContents = Integer.toString(useridx)+ ";" + id + ";" + pw;
            saveUserIdx = useridx;
            try {
                outputStream = openFileOutput("loginCache" , context.MODE_PRIVATE);
                outputStream.write(fileContents.getBytes());
                outputStream.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    String loginId;
    String loginPw;
    @JavascriptInterface
    public boolean askLogin(){
        Log.d(tag , "askLogin...");
        try{
            FileInputStream fis = openFileInput("loginCache");
            BufferedReader buffer = new BufferedReader(new InputStreamReader(fis));
            String str = buffer.readLine();
            saveUserIdx = Integer.parseInt(str.split(";")[0]);
            loginId = str.split(";")[1];
            loginPw = str.split(";")[2];
            // 크로스 스래드문제 때문에  Runnable 구현 후 처리
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl("javascript:setLoginInfo('"+loginId+"','"+loginPw+"')"); // 로그인 정보 세팅
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @JavascriptInterface
    public void delLogin(){
        File dir = getFilesDir();
        File file = new File(dir, "loginCache");
        file.delete();
    }

    // ===== 로그인 정보 저장 관련


    // 카메라 / 파일
    // 파일 관련 함수
    public final static int REQUEST_CODE_FILE = 801;
    private final int REQUEST_CODE_CAMERA = 802; // 응답
    private ValueCallback<Uri[]> filePathCallbackLollipop;
    private ValueCallback<Uri> filePathCallbackNormal;
    private Uri cameraImageUri = null;
    public final static int FILECHOOSER_NORMAL_REQ_CODE = 2001;
    public final static int FILECHOOSER_LOLLIPOP_REQ_CODE = 2002;
    // 권한 체킹

    private void readFile(){
        Log.d(tag , "function runCamera...");

        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        File path = getFilesDir();
        File file = new File(path, "fokCamera.png");
        // File 객체의 URI 를 얻는다.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            String strpa = getApplicationContext().getPackageName();
            cameraImageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", file);
        }
        else
        {
            cameraImageUri = Uri.fromFile(file);
        }
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

        // 선택팝업 카메라, 갤러리 둘다 띄우고 싶을 때..
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        pickIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        String pickTitle = "사진 가져올 방법을 선택하세요.";
        Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);

        // 카메라 intent 포함시키기..
        //chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{intentCamera});
        startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE);

    }

    //권한 획득 여부 확인
    @TargetApi(Build.VERSION_CODES.M)
    public void checkVerify() {
        Log.d(tag, "function check verify...");
        if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 카메라 또는 저장공간 권한 획득 여부 확인
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) || shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // 권한 요청 다이얼로그 표시
                new AlertDialog.Builder(this)
                        .setTitle("권한 요청")
                        .setMessage("이미지 파일을 촬영하거나 업로드하려면 권한이 필요합니다.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[]{Manifest.permission.INTERNET,
                                        Manifest.permission.ACCESS_NETWORK_STATE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.CAMERA}, REQUEST_CODE_FILE);
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 사용자가 권한을 거부하면 아무것도 하지 않음
                            }
                        })
                        .create()
                        .show();
            } else {
                // 권한 요청 다이얼로그 표시
                requestPermissions(new String[]{Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA}, REQUEST_CODE_FILE);
            }
        }
    }

    //권한 획득 여부에 따른 결과 반환
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        Log.d(tag , " ON NEW INTENT :::: onRequestPermissionsResult  requestCode : " + requestCode + " ,,,, REQUEST_CODE_FILE ::: " + REQUEST_CODE_FILE);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1)
        {
            if (grantResults.length > 0)
            {
                for (int i=0; i<grantResults.length; ++i)
                {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                    {
                        // 카메라, 저장소 중 하나라도 거부한다면 앱실행 불가 메세지 띄움
                        new AlertDialog.Builder(this).setTitle("알림").setMessage("권한을 허용해주셔야 앱을 이용할 수 있습니다.")
                                .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                }).setNegativeButton("권한 설정", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        getApplicationContext().startActivity(intent);
                                    }
                                }).setCancelable(false).show();
                        return;
                    }
                }
                //Toast.makeText(this, "Succeed Read/Write external storage !", Toast.LENGTH_SHORT).show();
                //startApp();
            }
        }
        if (requestCode == REQUEST_CODE_FILE) {
            checkVerify();
        }
    }


    // 뒤로가기
    private final long FINISH_INTERNAL_TIME = 2000;
    private long backPressedTime = 0;

    @Override
    public void onBackPressed() { //webView 뒤로가기버튼

        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        URL url = null;
        try {
            url = new URL(webView.getUrl());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Log.e("URL " , " webView.getUrl() : " + url.getHost());
        if (url.getHost().equals("abc-pokertest.co.kr") || url.getHost().equals("183.102.237.232")) { // 내부앱일경우
            // 모든페이지에서 뒤로가기시 무조건 앱종료 안내문구 => 앱종료
            webView.loadUrl("javascript:openMsgConfirmPop(1,'mpop_confirm','','어플을 종료하시겠습니까?' , 'javascript:window.android.gameEnd()')");
//            if (0 <= intervalTime && FINISH_INTERNAL_TIME - 500 >= intervalTime) finish();
//            else {
//                backPressedTime = tempTime;
//                Toast.makeText(getApplicationContext(), "한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
//                //webView.loadUrl("javascript:openMsgConfirmPop(1,'mpop_confirm','','어플을 종료하시겠습니까?' , 'javascript:window.android.gameEnd()')");
//            }
        } else { // 외부앱일경우 다시 뒤로가기
            webView.goBack();
        }
    }

    @JavascriptInterface
    public void gameEnd(){
        finish();
        System.exit(0);
    }


    // 결제
    public static Context mainContext;
    // 결제
    BillingManager billingManager;

    @JavascriptInterface
    public void buyBtn(String buyId){
        Log.e(tag , "BUY ID ::::::: " + buyId);
        billingManager.purchase(this , buyId);
    }

    public void sendResult(String itemNm, String itemMoney) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                Log.e(tag , "purchaseAjax..." + itemNm + " , " + itemMoney);
                webView.loadUrl("javascript:purchaseAjax('"+itemNm+"' ,'"+itemMoney+"')"); // 결제완룐
            }
        });
    };
}