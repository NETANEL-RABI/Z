package com.ytpremium.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText searchInput;
    private Button searchButton;
    private ListView resultsList;
    private ProgressBar progressBar;

    private final List<Video> videos = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchInput = findViewById(R.id.searchInput);
        searchButton = findViewById(R.id.searchButton);
        resultsList = findViewById(R.id.resultsList);
        progressBar = findViewById(R.id.progressBar);

        searchButton.setOnClickListener(v -> doSearch());

        resultsList.setOnItemClickListener((parent, view, position, id) -> {
            Video video = videos.get(position);
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            intent.putExtra("videoId", video.videoId);
            startActivity(intent);
        });
    }

    private void doSearch() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            return;
        }

        String apiKey = getString(R.string.youtube_api_key);
        if (apiKey.startsWith("PUT_YOUR")) {
            Toast.makeText(this, "צריך להגדיר מפתח YouTube API קודם ב-strings.xml", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String urlStr = "https://www.googleapis.com/youtube/v3/search"
                        + "?part=snippet"
                        + "&type=video"
                        + "&maxResults=20"
                        + "&q=" + encodedQuery
                        + "&key=" + apiKey;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONArray items = root.getJSONArray("items");

                List<Video> result = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String videoId = item.getJSONObject("id").getString("videoId");
                    JSONObject snippet = item.getJSONObject("snippet");
                    String title = snippet.getString("title");
                    String thumb = snippet.getJSONObject("thumbnails")
                            .getJSONObject("medium").getString("url");
                    result.add(new Video(videoId, title, thumb));
                }

                mainHandler.post(() -> {
                    videos.clear();
                    videos.addAll(result);
                    resultsList.setAdapter(new VideoAdapter());
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "שגיאה בחיפוש: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private class VideoAdapter extends ArrayAdapter<Video> {
        VideoAdapter() {
            super(MainActivity.this, R.layout.item_video, videos);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = getLayoutInflater().inflate(R.layout.item_video, parent, false);
            }
            Video video = videos.get(position);

            TextView title = row.findViewById(R.id.title);
            title.setText(video.title);

            ImageView thumb = row.findViewById(R.id.thumbnail);
            loadThumbnail(thumb, video.thumbnailUrl);

            return row;
        }
    }

    private void loadThumbnail(ImageView imageView, String urlStr) {
        imageView.setImageDrawable(null);
        executor.execute(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.connect();
                java.io.InputStream input = conn.getInputStream();
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                mainHandler.post(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception ignored) {
                // אם תמונה ממוזערת לא נטענת (למשל נחסמה ע"י סינון), פשוט מדלגים
            }
        });
    }
  }
