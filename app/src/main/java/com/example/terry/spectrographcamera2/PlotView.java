package com.example.terry.spectrographcamera2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PlotView extends View {
    float[] numbers;
    Paint p;
    int height;
    int width;

    public PlotView(Context context) {
        super(context);
        p = new Paint();
    }

    public PlotView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(15f);
    }


    public void setNumbers(float[] floats){
        for(float i :floats){
            i = i/255*height;
        }
        numbers = floats;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        width = w;
        height = h;
    }

    @Override
    public void onDraw(Canvas canvas){
        if(numbers!=null){
            canvas.drawLines(numbers,p);
        }else{
            canvas.drawText("no Stastics",width/2,height/2,p);
        }
    }
}
