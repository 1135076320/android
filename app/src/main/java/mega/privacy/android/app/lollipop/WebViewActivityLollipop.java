package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;


public class WebViewActivityLollipop extends Activity {

    private WebView myWebView;
    private ProgressDialog progressDialog;
    private Activity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fortumo_payment);

        activity = this;
        progressDialog = ProgressDialog.show(activity, this.getString(R.string.embed_web_browser_loading_title), this.getString(R.string.embed_web_browser_loading_message), true);
        progressDialog.setCancelable(false);

        myWebView = findViewById(R.id.webview);


        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                progressDialog.show();
                view.loadUrl(url);

                return true;
            }

            @Override
            public void onPageFinished(WebView view, final String url) {
                progressDialog.dismiss();
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getDataString();
            log("URL: " + url);
            myWebView.loadUrl(url);
        }
    }

    public static void log(String message) {
        Util.log("WebViewActivityLollipop", message);
    }
}
