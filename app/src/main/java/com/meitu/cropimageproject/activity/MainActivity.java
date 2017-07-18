package com.meitu.cropimageproject.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.meitu.cropimageproject.R;

public class MainActivity extends AppCompatActivity {

    public static final int GET_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }


    public void onClick(View v) {
        //就一个按钮
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, GET_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GET_IMAGE:
                    Uri uri = data.getData();
                    Intent intent = new Intent(this, DisplayActivity.class);
                    intent.putExtra("uri", uri);
                    startActivity(intent);
                    break;
            }
        }
    }
}
