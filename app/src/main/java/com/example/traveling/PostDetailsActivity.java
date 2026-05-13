package com.example.traveling;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class PostDetailsActivity extends AppCompatActivity {

    ImageView imageView;
    TextView captionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        imageView = findViewById(R.id.imageView);
        captionView = findViewById(R.id.caption);

        String caption = getIntent().getStringExtra("caption");
        String imageUrl = getIntent().getStringExtra("imageUrl");

        captionView.setText(caption);

        Glide.with(this)
                .load(imageUrl)
                .into(imageView);
    }
}