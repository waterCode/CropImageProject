package com.meitu.cropimageproject.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.meitu.cropimageproject.R;
import com.meitu.cropimageproject.view.CropImageView;

/**
 * Created by zmc on 2017/7/18.
 */

public class DisplayActivity extends AppCompatActivity {
    private Uri uri;
    private CropImageView mNeedCropView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_iamge_activity);
        mNeedCropView = (CropImageView) findViewById(R.id.crop_photo);


        Drawable drawable = getResources().getDrawable(R.drawable.screenshot);
        mNeedCropView.setImageDrawable(drawable);
        //mNeedCropView.setDrawable(drawable);
        /*Intent intent = getIntent();

        uri = intent.getParcelableExtra("uri");
        if(uri !=null) {
            Glide.with(this).load(uri).into(mNeedCropView);
        }*/
    }


}
