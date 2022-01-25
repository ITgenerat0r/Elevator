package com.example.elevator.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Locale;


// This class used for discover services for GATT
public class ConnectThreadLE extends Thread {
    private BluetoothGatt gatt;
    private int delay;
    public ConnectThreadLE(BluetoothGatt gatt, int delay){
        this.gatt = gatt;
        this.delay = delay;
    }

    //
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void run() {
        Log.d("MainLog", String.format(Locale.ENGLISH, "Discovering services with delay of %d ms", delay));
        boolean result = gatt.discoverServices();
        if(!result){
            Log.e("MainLog", "discoverServices failed to start");
        }
//        super.run();
    }
}
