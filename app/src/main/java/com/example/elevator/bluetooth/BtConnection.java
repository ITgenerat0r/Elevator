package com.example.elevator.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.Toast;

import com.example.elevator.adapter.BtAdapter;
import com.example.elevator.adapter.BtConsts;

// Класс для инициализации подключения
public class BtConnection {
    private Context context;
    private SharedPreferences preferences;
    private BluetoothAdapter btAdapter;
    private BluetoothDevice device;
    private ConnectThread connectThread;
    private boolean connection_state;

    // Конструктор
    public BtConnection(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        connection_state = false;
    }

    // подключение к BlueTooth устройству
    public void connect(){
        connection_state = true;
        // Достаем из памяти МАС адресс
        String mac = preferences.getString(BtConsts.MAC_KEY, "");
        if(!btAdapter.isEnabled() || mac.isEmpty()) return;

        device = btAdapter.getRemoteDevice(mac);
        // Если null, то устройство к которому подключаемся недоступно
        if(device == null) return;
        Toast toast = Toast.makeText( this.context , "Try connect to " + device.getAddress(), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 10 , 20);
        toast.show();
        connectThread = new ConnectThread(context, btAdapter, device);
        connectThread.start();
    }

    public void SendMessage(String message){
        connectThread.getReceiveThread().sendMessage(message.getBytes());
    }
}
