//package com.example.myapplication;
//
//import android.annotation.SuppressLint;
//import android.os.Bundle;
//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;
//import androidx.appcompat.app.AppCompatActivity;
//
//public class WebViewActivity extends AppCompatActivity {
//    private WebView webView;
//
//    @SuppressLint("SetJavaScriptEnabled")
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_webview);
//
//        webView = findViewById(R.id.webView);
//        WebSettings webSettings = webView.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.setDomStorageEnabled(true);
//
//        webView.setWebViewClient(new WebViewClient());
//        String url = getIntent().getStringExtra("url");
//
//        if (url != null) {
//            webView.loadUrl(url);
//        }
//    }
//}
//package com.example.myapplication;
//
//import android.annotation.SuppressLint;
//import android.os.Bundle;
//import android.util.Log;
//import android.webkit.WebChromeClient;
//import android.webkit.WebResourceRequest;
//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//public class WebViewActivity extends AppCompatActivity {
//    private WebView webView;
//    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
//
//    @SuppressLint("SetJavaScriptEnabled")
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_webview);
//
//        webView = findViewById(R.id.webView);
//
//        // Enable JavaScript
//        WebSettings webSettings = webView.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.setDomStorageEnabled(true); // Enable local storage
//        webSettings.setAllowFileAccess(true);
//        webSettings.setAllowContentAccess(true);
//        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
//        webSettings.setMediaPlaybackRequiresUserGesture(false);
//
//        // Load the URL
//        String url = getIntent().getStringExtra("url");
//        if (url != null) {
//            Log.d("WebViewDebug", "Loading URL in WebView: " + url);
//            webView.loadUrl(url);
//        } else {
//            Log.e("WebViewError", "No URL received");
//        }
//
//        // Force WebView to open URLs inside the app
//        webView.setWebViewClient(new WebViewClient() {
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//                view.loadUrl(request.getUrl().toString());
//                return true;
//            }
//        });
//
//        // Handle JavaScript alerts & permissions
//        webView.setWebChromeClient(new WebChromeClient());
//    }
//}
package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class WebViewActivity extends AppCompatActivity {
    private WebView webView;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);

        // Enable JavaScript and other necessary settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Handle camera permissions for WebView
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            request.grant(request.getResources());
                        } else {
                            ActivityCompat.requestPermissions(WebViewActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                        }
                    } else {
                        request.grant(request.getResources());
                    }
                });
            }
        });

        webView.setWebViewClient(new WebViewClient());

        // Load the URL from the intent
        String url = getIntent().getStringExtra("url");
        if (url != null) {
            Log.d("WebViewDebug", "Loading URL: " + url);
            webView.loadUrl(url);
        } else {
            Log.e("WebViewError", "No URL received");
        }
    }

    // Handle camera permission result
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("WebViewDebug", "Camera permission granted");
            } else {
                Log.e("WebViewError", "Camera permission denied");
            }
        }
    }
}
