package com.tongin.yeoksam.customerdb;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST = 100;
    private static final String APP_URL = "https://kaleidoscopic-babka-d2f816.netlify.app/";
    private WebView webView;
    private boolean pageReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);
        configureWebView();
        requestNeededPermissions();
        webView.loadUrl(APP_URL);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new NativeBridge(), "TonginNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = true;
                deliverPendingSms();
            }
        });
    }

    private void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        List<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECEIVE_SMS);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]), PERMISSION_REQUEST);
    }

    @Override protected void onResume() {
        super.onResume();
        if (pageReady) deliverPendingSms();
    }

    private void deliverPendingSms() {
        String sms = getSharedPreferences("tongin_sms", MODE_PRIVATE).getString("pending_sms", "");
        if (sms.isEmpty()) return;
        String quoted = JSONObject.quote(sms);
        String js = "(function(){" +
            "var input=document.getElementById('smsInput');" +
            "if(!input||typeof window.parseSMS!=='function')return false;" +
            "input.value=" + quoted + ";" +
            "if(typeof window.showPage==='function'&&typeof window.navButton==='function')window.showPage('add',window.navButton('add'));" +
            "window.parseSMS();return true;})();";
        webView.evaluateJavascript(js, result -> {
            if ("true".equals(result)) {
                getSharedPreferences("tongin_sms", MODE_PRIVATE).edit().remove("pending_sms").apply();
                Toast.makeText(this, "문자 내용을 불러왔습니다. 확인 후 등록해 주세요.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    public class NativeBridge {
        @JavascriptInterface public String appMode() { return "android-sms-local"; }
    }
}
