package com.example.smartlock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;


public class TemperatureView extends View {
    public double temperature=0.0;
    public TemperatureView(Context context)
    {
        super(context);
    }

    public TemperatureView(Context context, AttributeSet attrs)
    {
        super(context,attrs);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        double tmp=temperature;
        if(tmp>100) tmp = 100.0;
        else if(tmp<-100) tmp = -100;
        Paint paint=new Paint();
        paint.setColor(Color.BLACK);
        int y=10;
        for(int scale=100;scale>=-100;scale-=5)
        {
            canvas.drawLine(10, y,100,y,paint);
            canvas.drawText(Integer.toString(scale), 110, y + 6, paint);
            y+=25;
        }
        int tmpy = y  - ((int)((((tmp+100.0)/5.0f))*25.0f)) -25;
        if(tmp>20 || tmp<5) paint.setColor(Color.RED);
        else paint.setColor(Color.GREEN);
        paint.setStrokeWidth(5);
        canvas.drawLine(10, tmpy, 200, tmpy, paint);
        canvas.drawCircle(200, tmpy, 40, paint);
        String temp=Double.toString(tmp);
        Rect trect = new Rect();

        paint.setColor(Color.BLACK);
        paint.setTextSize(24);
        paint.getTextBounds(temp,0,temp.length(),trect);
        canvas.drawText(temp, 200 - trect.width()/2,tmpy + trect.height()/2, paint);
    }

}
