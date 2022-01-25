package com.example.elevator.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReceiveThread extends Thread {
    private BluetoothSocket socket;
    private InputStream inputS;
    private OutputStream outputS;
    private byte[] rBuffer;


    public ReceiveThread(BluetoothSocket socket){
        this.socket = socket;
        try {
            inputS = socket.getInputStream();
        } catch (IOException e){
            //
        }
        try {
            outputS = socket.getOutputStream();
        } catch (IOException e){
            //
        }
    }

    @Override
    public void run() {
        rBuffer = new byte[2]; // Передача будев по 2 байта
        while (true){
            try {
                int l = inputS.read(rBuffer);
                String message = new String(rBuffer, 0, l);
                Log.d("MainLog", "Received data: " + message);
            } catch (IOException e){
                break;
            }
        }
    }

    public void sendMessage(byte[] bytes){
        try {
            outputS.write(bytes);
        } catch (IOException e){
            //
        }
    }
}
