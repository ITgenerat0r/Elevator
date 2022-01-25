package com.example.elevator.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

// Класс для подключения
public class ConnectThread extends Thread {
    private Context context;
    private BluetoothAdapter btAdapter;
    private BluetoothDevice device;
    private BluetoothSocket mSocket;
    private ReceiveThread receiveThread;
    public static final String UUID = "00001101-0000-1000-8000-00805F9B34FB";


    public ConnectThread (Context context, BluetoothAdapter btAdapter, BluetoothDevice device){
        this.context = context;
        this.btAdapter = btAdapter;
        this.device = device;
        try {
            mSocket = device.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID));
        } catch (IOException e) {
            //
        }
    }


    // Запуск вторым потоком
    @Override
    public void run() {
        btAdapter.cancelDiscovery();

        try {
            MsgBox("Try connect to " + device.getAddress(), "log");
            mSocket.connect();
            MsgBox("Connected to " + device.getAddress(), "log");
            receiveThread = new ReceiveThread(mSocket);
            receiveThread.start();
        } catch (IOException e){
            MsgBox("Not connected to " + device.getAddress(), "log");
            closeConnection();
        }
    }


    public void closeConnection(){
        try {
            mSocket.close();
            MsgBox("Connection closed. (" + device.getAddress() + ")", "log");
        } catch (IOException e){
            //
        }
    }

    private void MsgBox(String message, String n){
        if(n == "log" || n == "all") Log.d("MainLog", message);
        if(n == "toast" || n == "all") {
            Toast toast = Toast.makeText(this.context, message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 10, 20);
            toast.show();
        }
    }

    public ReceiveThread getReceiveThread() {
        return receiveThread;
    }
}
