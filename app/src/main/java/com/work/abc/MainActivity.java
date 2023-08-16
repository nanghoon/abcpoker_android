package com.work.abc;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    //private String url_main = "http://1.234.66.89/abcpoker/allPage.do";
    private String url_main = "http://183.102.237.232:8080/abcpoker/allPage.do";

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
        webView.getSettings().setSupportMultipleWindows(true); // 카카오톡 설정
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 화면 세로고정
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) { // 페이지 컨트롤을 위한 기본적인 함수
                if(url.startsWith("intent:")){ // 카카오톡 설정
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                        if (existPackage != null) {
                            startActivity(intent);
                        } else {
                            Log.d("ABC POKER ", "Could not parse anythings");
//                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
//                            marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
//                            startActivity(marketIntent);
                        }

                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    view.loadUrl(url);
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() { // js alert 띄우기 위해 추가해야함
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
        Log.d("ABC POKER " , "Call Restart...");
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        startActivity(mainIntent);
        System.exit(0);
    }

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
        Log.d("ABC POKER APP " , "function runCamera...");

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
        Log.d("ABC POKER APP", "function check verify...");
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
        Log.d("dogAPP" , " ON NEW INTENT :::: onRequestPermissionsResult  requestCode : " + requestCode + " ,,,, REQUEST_CODE_FILE ::: " + REQUEST_CODE_FILE);
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
        if (url.getHost().equals("1.234.66.89") || url.getHost().equals("183.102.237.232")) { // 내부앱일경우
            // 모든페이지에서 뒤로가기시 무조건 앱종료 안내문구 => 앱종료
            if (0 <= intervalTime && FINISH_INTERNAL_TIME - 500 >= intervalTime) finish();
            else {
                backPressedTime = tempTime;
                Toast.makeText(getApplicationContext(), "한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
                //webView.loadUrl("javascript:openMsgConfirmPop(1,'mpop_confirm','','어플을 종료하시겠습니까?' , 'javascript:window.android.gameEnd()')");
            }
        } else { // 외부앱일경우 다시 뒤로가기
            webView.goBack();
        }
    }

    @JavascriptInterface
    public void gameEnd(){
        finish();
        System.exit(0);
    }
}