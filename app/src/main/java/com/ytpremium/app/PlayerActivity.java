package com.ytpremium.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class PlayerActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        String videoId = getIntent().getStringExtra("videoId");

        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // נדרש כדי שנגן ה-iframe הרשמי של יוטיוב יוכל למלא את המסך בווידאו
        webView.setWebChromeClient(new WebChromeClient());

        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        String html = buildPlayerHtml(videoId);

        // טוענים מתוך youtube.com עצמו (baseUrl) כדי שה-IFrame Player API
        // הרשמי יעבוד כמו בדפדפן רגיל, ולא ייחסם כתוכן ממקור לא מוכר
        webView.loadDataWithBaseURL(
                "https://www.youtube.com",
                html,
                "text/html",
                "utf-8",
                null);
    }

    private String buildPlayerHtml(String videoId) {
        return "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>body{margin:0;background:#000;}#player{width:100%;height:100%;position:fixed;top:0;left:0;}</style>"
                + "</head><body>"
                + "<div id='player'></div>"
                + "<script src='https://www.youtube.com/iframe_api'></script>"
                + "<script>"
                + "var player;"
                + "function onYouTubeIframeAPIReady(){"
                + "  player = new YT.Player('player', {"
                + "    videoId: '" + videoId + "',"
                + "    playerVars: { 'playsinline': 1, 'autoplay': 1 },"
                + "    events: {}"
                + "  });"
                + "}"
                + "</script>"
                + "</body></html>";
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
