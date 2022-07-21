package com.example.elevator.objects;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Control {
    String TAG = this.getClass().getSimpleName();


    public boolean check(){
        if(!dt()) return false;
        return true;
    }

    private boolean dt(){
        Date cur = Calendar.getInstance(TimeZone.getTimeZone("Russia/Moscow")).getTime();
        int y = cur.getYear() + 1900;
        int m = cur.getMonth() + 1;
        int yy = 2023;
        int mm = 2;
//        Log.d(TAG, "DATE YEAR " + cur.toString().substring(cur.toString().length()-4));
        if(y > yy){
            return false;
        } else if (y < yy) {
            return true;
        } else if (m > mm){
            return false;
        }
        return true;
    }
}
