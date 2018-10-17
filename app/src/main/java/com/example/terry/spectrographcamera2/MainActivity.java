package com.example.terry.spectrographcamera2;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    String[] PERMISSIONS = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA",
    };

    static {
        System.loadLibrary("native-lib");
        //System.loadLibrary("opencv_java3");
    }
    private static int REQUEST_ORIGINAL=0;// 请求图片信号标识
    private static int CROP_PHOTO = 1;
    Uri uri;
    Button captureBtn;
    Button absorbBtn;
    Button saveBtn;
    Button sourceBtn;
    ImageView imageView;
    PlotView plotView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for(String per:PERMISSIONS){
            if(checkSelfPermission(per)!= PackageManager.PERMISSION_GRANTED ){
                requestPermissions(PERMISSIONS,1);
            }
        }


        captureBtn = findViewById(R.id.capture);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v == captureBtn){
                    //调用系统相机的Intent
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    //临时照片文件的位置

                    //将照片文件的位置传入intent
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    //发出intent启动系统相机
                    startActivityForResult(intent, REQUEST_ORIGINAL);
                }
            }
        });

        sourceBtn = findViewById(R.id.source);
        saveBtn = findViewById(R.id.save);
        absorbBtn = findViewById(R.id.source);
        imageView = findViewById(R.id.image);
        plotView = findViewById(R.id.plot);

        String filePath = Environment.getExternalStorageDirectory() + "/images/temp.jpg";
        File outputFile = new File(filePath);
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdir();
        }

        uri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".myprovider",
                outputFile);
        //grantUriPermission(getPackageName(),uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //grantUriPermission(getPackageName(),uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    //系统相机返回时的回调方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ORIGINAL) {
                Intent intent = new Intent("com.android.camera.action.CROP"); //剪裁
                intent.setDataAndType(uri, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION); //添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.putExtra("scale", true);
                //设置宽高比例
                intent.putExtra("aspectX", 1);
                intent.putExtra("aspectY", 1);
                //设置裁剪图片宽高
                intent.putExtra("outputX", 800);
                intent.putExtra("outputY", 400);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                Toast.makeText(MainActivity.this, "剪裁图片", Toast.LENGTH_SHORT).show();
                //广播刷新相册
                Intent intentBc = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intentBc.setData(uri);

                this.sendBroadcast(intentBc);
                startActivityForResult(intent, CROP_PHOTO);
            } else if (requestCode == CROP_PHOTO) {
                //从临时照片文件的位置加载照片
                Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/images/temp.jpg");
                //将图片设置给ImageView显示
                imageView.setImageBitmap(bitmap);
                float[] result = getBitmapSpetrograph(bitmap);
                plotView.setNumbers(result);
//                double greyLevl = getBitmapMeanGray(bitmap);
//                double concentration = -145.7491 + 1.27 * greyLevl;
//                if (concentration < 0) concentration = 0;
//                greyLevelTextView.setText(String.format("%.2f", greyLevl));
//                concentrationTextView.setText(String.format("%.2f", concentration));
            }
        }
    }



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    public native double getBitmapMeanGray(Bitmap bitmap);

    public native float[] getBitmapSpetrograph(Bitmap bitmap);
}
