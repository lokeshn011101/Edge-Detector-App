package com.example.edgedetector;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

public class ImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        Bundle extras = getIntent().getExtras();
        ImageView imageView = (ImageView) findViewById(R.id.imageViewImageIntent);
        Picasso.get().load(extras.getString("url")).placeholder(R.drawable.ic_launcher_background).into(imageView);
    }
}