package com.example.mrokade.asanaalarm;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by mrokade on 3/14/16.
 */
public class WebAuthActivity extends Activity {
    private static final String oauthUrl = "https://app.asana.com/-/oauth_authorize?response_type=code&client_id=97636893982290&redirect_uri=https://www.asanaalarm.co/oauth&state=ok";
    public final OkHttpClient client = new OkHttpClient();
    public final String redirectUrl = "https://www.asanaalarm.co/oauth";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_activity);
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        WebView webview = (WebView) findViewById(R.id.webView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setLoadsImagesAutomatically(true);

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (Uri.parse(url).getHost().equals("www.asanaalarm.co")) {
                    String code = Uri.parse(url).getQueryParameter("code");
                    Log.i("GOT TOKEN", code);

                    final SharedPreferences.Editor edit = sharedPref.edit();
                    edit.putString("ASANA_GRANT_CODE", code);
                    RequestBody formBody = new FormBody.Builder()
                            .add("grant_type", "authorization_code")
                            .add("client_id", Constants.CLIENT_ID)
                            .add("client_secret", Constants.CLIENT_SECRET)
                            .add("redirect_uri", redirectUrl)
                            .add("code", code)
                            .build();

                    Request request = new Request.Builder()
                            .url("https://app.asana.com/-/oauth_token")
                            .post(formBody)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String res = response.body().string();
                            Log.i("CODE_GRANT", res);
                            JSONObject resJson;
                            try {
                                resJson = new JSONObject(res);
                                String token = resJson.get("access_token").toString();
                                String refreshToken = resJson.get("refresh_token").toString();
                                edit.putString("ASANA_ACCESS_TOKEN", token);
                                edit.putString("ASANA_REFRESH_TOKEN", refreshToken);
                                Date date = new Date();
                                edit.putString("ASANA_TOKEN_TIMESTAMP", date.toString());
                                edit.apply();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    view.loadUrl(url);
                }
                finish();

                return true;
            }
        });
        webview.loadUrl(oauthUrl);
    }
}
