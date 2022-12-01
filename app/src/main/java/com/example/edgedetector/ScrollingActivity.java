package com.example.edgedetector;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ScrollingActivity extends AppCompatActivity {

    public TreeMap<String, String> nameUrls = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Request request = new Request.Builder()
                .url("http://192.168.56.1:5000/get_all_images")
                .build();

        final OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.scrolllinearlayout);

        call.enqueue(new Callback() {
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                assert response.body() != null;
                String jsonData = response.body().string();
                Log.d("api call", jsonData);
                try {
                    JSONArray jsonArray = new JSONArray(jsonData);
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String imageName = jsonObject.get("name").toString();
                        String imageUrl = jsonObject.get("url").toString();
                        nameUrls.put(imageName, imageUrl);
                        Button newButton = new Button(getApplicationContext());
                        newButton.setOnClickListener((View view) -> {
                            viewImage(view);
                        });
                        newButton.setText(imageName);
                        newButton.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
                        newButton.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
                        runOnUiThread(() -> linearLayout.addView(newButton));

                        Log.d("jarrname", jsonObject.get("name").toString());
                        Log.d("jarrurl", jsonObject.get("url").toString());
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            public void onFailure(Call call, IOException e) {
                Log.d("api call", "api call fail");
            }
        });
    }

    public void viewImage(View view) {
        Intent imageIntent = new Intent(ScrollingActivity.this, ImageActivity.class);
        Button currentButton = (Button) view;
        String imageName = String.valueOf(currentButton.getText());
        imageIntent.putExtra("name", imageName);
        imageIntent.putExtra("url", nameUrls.get(imageName));
        startActivity(imageIntent);
    }
}