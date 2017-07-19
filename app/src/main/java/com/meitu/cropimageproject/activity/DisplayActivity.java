package com.meitu.cropimageproject.activity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.meitu.cropimageproject.R;
import com.meitu.cropimageproject.util.ImageLoadUtil;
import com.meitu.cropimageproject.view.CropImageView;

import java.io.FileNotFoundException;

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
        uri = getIntent().getParcelableExtra("uri");

        Bitmap bitmap = null;
        try {
            bitmap = ImageLoadUtil.loadImage(getContentResolver(), uri);
            mNeedCropView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //mNeedCropView.setDrawable(drawable);
        /*Intent intent = getIntent();

        uri = intent.getParcelableExtra("uri");
        if(uri !=null) {
            Glide.with(this).load(uri).into(mNeedCropView);
        }*/
    }


}
