package com.example.terry.spectrographcamera2;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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

import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends Activity {

    float[] nowPresent;
    float[] source;
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
    XYPlot plotView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for(String per:PERMISSIONS){
            if(checkSelfPermission(per)!= PackageManager.PERMISSION_GRANTED ){
                requestPermissions(PERMISSIONS,1);
            }
        }

        View.OnClickListener onClickListener = new View.OnClickListener() {
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
                }else if(v==absorbBtn){
                    if(source==null){
                        Toast.makeText(MainActivity.this,"no source",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for(int i=0;i<nowPresent.length;i++){
                        nowPresent[i] = (float) Math.log(source[i]/nowPresent[i]);
                    }
                    setOnUI(nowPresent);
                }else if(v==saveBtn){
                    Toast.makeText(MainActivity.this,"saved",Toast.LENGTH_SHORT).show();
                }else if(v==sourceBtn){
                    source = nowPresent;
                    if(nowPresent==null||source==null)
                        Toast.makeText(MainActivity.this,"NULL!!",Toast.LENGTH_SHORT).show();
                    else Toast.makeText(MainActivity.this,"set source",Toast.LENGTH_SHORT).show();
                }
            }
        };
        captureBtn = findViewById(R.id.capture);
        captureBtn.setOnClickListener(onClickListener);

        sourceBtn = findViewById(R.id.source);
        sourceBtn.setOnClickListener(onClickListener);
        saveBtn = findViewById(R.id.save);
        saveBtn.setOnClickListener(onClickListener);
        absorbBtn = findViewById(R.id.absorb);
        absorbBtn.setOnClickListener(onClickListener);
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
                nowPresent = getBitmapSpetrograph(bitmap);
                setOnUI(nowPresent);
            }
        }
    }


    public void setOnUI(float[] floats){
        double[] smooth = linearSmooth5(floats,floats.length);
        ArrayList<Number> list = new ArrayList<>();
        for(double i:smooth ){
            list.add(i);
        }
        XYSeries series1 = new SimpleXYSeries(
                list,// SimpleXYSeries takes a List so turn our array into a List
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,// Y_VALS_ONLY means use the element index as the x value
                "Spectrograph");// Set the display title of the series);
//                LineAndPointFormatter series1Format = new LineAndPointFormatter(
//                        Color.rgb(0, 200, 0), // line color
//                        Color.rgb(0, 100, 0), // point color
//                        Color.rgb(150, 190, 150),null);
        LineAndPointFormatter series1Format = new LineAndPointFormatter(
                this,R.xml.line_point_formatter_with_labels);
        series1Format.setInterpolationParams(
                new CatmullRomInterpolator.Params(100, CatmullRomInterpolator.Type.Centripetal));
        plotView.clear();
        plotView.addSeries(series1, series1Format);
        plotView.invalidate();
//                double greyLevl = getBitmapMeanGray(bitmap);
//                double concentration = -145.7491 + 1.27 * greyLevl;
//                if (concentration < 0) concentration = 0;
//                greyLevelTextView.setText(String.format("%.2f", greyLevl));
//                concentrationTextView.setText(String.format("%.2f", concentration));
    }
    public double[] linearSmooth5 ( float in[], int N )
    {
        double out[] = new double[N];
        int i;
        if ( N < 5 )
        {
            for ( i = 0; i <= N - 1; i++ )
            {
                out[i] = in[i];
            }
        }
        else
        {
            out[0] = ( 3.0 * in[0] + 2.0 * in[1] + in[2] - in[4] ) / 5.0;
            out[1] = ( 4.0 * in[0] + 3.0 * in[1] + 2 * in[2] + in[3] ) / 10.0;
            for ( i = 2; i <= N - 3; i++ )
            {
                out[i] = ( in[i - 2] + in[i - 1] + in[i] + in[i + 1] + in[i + 2] ) / 5.0;
            }
            out[N - 2] = ( 4.0 * in[N - 1] + 3.0 * in[N - 2] + 2 * in[N - 3] + in[N - 4] ) / 10.0;
            out[N - 1] = ( 3.0 * in[N - 1] + 2.0 * in[N - 2] + in[N - 3] - in[N - 5] ) / 5.0;
        }
        return out;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    public native float[] getBitmapSpetrograph(Bitmap bitmap);
}
