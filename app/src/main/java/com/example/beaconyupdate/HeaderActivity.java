package com.example.beaconyupdate;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class HeaderActivity extends AppCompatActivity {
    private CountDownTimer countDownTimer;
    private ImageView img_header;
    private Animation slide_img;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.header_activity);

        img_header=findViewById(R.id.img_header);
        slide_img = AnimationUtils.loadAnimation(HeaderActivity.this,R.anim.slide_img);

        countDownTimer=new CountDownTimer(2000,2000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                Intent i=new Intent(HeaderActivity.this, MainActivity.class);
                startActivity(i);
                HeaderActivity.this.finish();
            }
        }.start();
        img_header.startAnimation(slide_img);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
