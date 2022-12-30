package com.example.elevator.bluetoothle;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.elevator.R;
import com.example.elevator.adapter.BtConsts;
import com.example.elevator.bluetooth.ConnectThread;
import com.example.elevator.objects.Device;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.ERROR;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
//import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
//import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;



public class BLEConnection<IBluetoothGatt> implements BluetoothProfile {
    private static final String TAG = "BLEConnection";
    private boolean state_notify = false;

    private IBluetoothGatt mService; // Сомнительная *****
    private int connect = STATE_DISCONNECTED;

    private ConnectThreadLE threadConnect;
    private Context context;
    private SharedPreferences preferences;
    private BluetoothAdapter btAdapter;
    private BluetoothDevice device;
    private ConnectThread connectThread;
    private BluetoothGatt gatt;

    private BluetoothGattCharacteristic character;
    private List<BluetoothGattService> services;
    private String mac;
    private boolean character_state = false;

    private int nrTries = 0;
    private boolean isRetrying = false;
    private List<String> response;
    private boolean isGetResponse = false; // true if we got data from arduino

    private static final int AUTHENTICATION_NONE = 0;
    private static final int MAX_TRIES = 5;

    private boolean isConnecting;
    private boolean isSending; // true - while sending message

    private Device connectedDevice = null;
    public List<String> getResponse() {
        return response;
    }

    public boolean isConnecting(){ return isConnecting; }

    public void clearResponse(){
        response.clear();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private final BluetoothGattCallback btGattCallBack = new BluetoothGattCallback() {



        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            MsgBox("onCharacteristicRead()", "log");
            if(status != GATT_SUCCESS){
                MsgBox(String.format(Locale.ENGLISH, "ERROR: Read failed for characteristic: %s, status %d", characteristic.getUuid(), status), "log");
//                completedCommand();
                return;
            }
//            if(VDBG){
//                Log.d(TAG, "onCharacteristicRead() - Device=" + gatt.getDevice().getAddress() + " handle=" + "handle" + " Status=" + status);
//            }
            if(!gatt.getDevice().getAddress().equals(device.getAddress())){
                return;
            }

//            synchronized (mDeviceBusyLock){ // I have no idea, what this is.
//                mDeviceBusy = false;
//            }
//            Log.d(TAG, String.format("      UUID:%s, Type:%s", characteristic.getUuid(), characteristic.getWriteType()));
//            Log.d(TAG, String.format("         Value: %s", characteristic.getValue()));
//            Log.d(TAG, String.format("         Value (string): %s", characteristic.getStringValue(0)));
//            Log.d(TAG, String.format("         Property=%d, Permission=%d", characteristic.getProperties(), characteristic.getPermissions()));
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d(TAG,"Read data: " + stringBuilder.toString());
            }
//            completedCommand();
//            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            MsgBox("onCharacteristicWrite()", "log");
//            Log.d(TAG, String.format("      UUID:%s, Type:%s", characteristic.getUuid(), characteristic.getWriteType()));
//            Log.d(TAG, String.format("         Property=%d, Permission=%d", characteristic.getProperties(), characteristic.getPermissions()));

//            super.onCharacteristicWrite(gatt, characteristic, status);
        }

//        @SuppressLint("DefaultLocale")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            MsgBox("onConnectionStateChange()", "log");
            if(status == GATT_SUCCESS){
                Log.d(TAG, "GATT_SUCCESS, newState="+newState);
//                MsgBox(String.format("%d", STATE_CONNECTED), "log");
                if(newState == STATE_CONNECTED) {
                    MsgBox("STATE_CONNECTED", "log");
                    int bondState = device.getBondState();
                    // Обрабатываем bondState
                    if (bondState == BOND_NONE || bondState == BOND_BONDED){
                        connect = STATE_CONNECTED;
                        isConnecting = false;
                        connectedDevice.setAddress(gatt.getDevice().getAddress());
                        connectedDevice.setName(gatt.getDevice().getName());
                        if(connectedDevice.getName() == null) Log.d(TAG, "connectedDevice.Name is null");
                        Log.d(TAG, String.format("connectedDevice.Name = %s", connectedDevice.getName()));
                        MsgBox(context.getString(R.string.succesful_connect)+" "+connectedDevice.getName()+" ("+connectedDevice.getAddress()+")", "log");
                        response.add(context.getString(R.string.succesful_connect)+" "+connectedDevice.getName()+" ("+connectedDevice.getAddress()+")");

                        // Мы подключились к устройству, вызываем discoverServices с задержкой
                        int delayWhenBonded = 0;
                        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.N){
                            delayWhenBonded = 1000;
                        }
                        final int delay = bondState == BOND_BONDED ? delayWhenBonded : 0;
                        Log.d(TAG, "Delay=" + delay);
                        threadConnect = new ConnectThreadLE(gatt, delay);
                        threadConnect.start();
//
                        Log.d(TAG, "End discover lines");
                    } else if (bondState == BOND_BONDING){
                        Log.i("MainLog", "Waiting for bonding to complete");
                    }
                } else if(newState == STATE_DISCONNECTED){
                    //
                    gatt.close();
                    connect = STATE_DISCONNECTED;
                }
            } else {
                // Произошла ошибка
            }
//            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            MsgBox("onServicesDiscovered()", "log");
            if(status == ERROR){
                Log.e("MainLog", "Service discovery failed");
                disconnect();
                return;
            }
            services = gatt.getServices();
            Log.d(TAG, String.format(Locale.ENGLISH, "discovered %d services for '%s'", services.size(), device.getName()));
//            for (int i = 0; i<services.size(); i++){
//                List<BluetoothGattCharacteristic> characteristics = services.get(i).getCharacteristics();
//                Log.d(TAG, String.format("%d service - %d characteristics", i+1, characteristics.size()));
//                for(int j = 0; j < characteristics.size(); j++){
//                    BluetoothGattCharacteristic characteristic = characteristics.get(j);
//                    Log.d(TAG, String.format("     %d) UUID:%s, Type:%s", j+1, characteristic.getUuid(), characteristic.getWriteType()));
//                    Log.d(TAG, String.format("         Value: %s", characteristic.getValue()));
//                    Log.d(TAG, String.format("         Value (string): %s", characteristic.getStringValue(0)));
//                    Log.d(TAG, String.format("         Property=%d, Permission=%d", characteristic.getProperties(), characteristic.getPermissions()));
//                }
//            }
            character = services.get(2).getCharacteristics().get(0);
            Log.d(TAG, "character = " + character);
            if(services.size() > 0)  character_state = true;
//            BluetoothGattService test = services.get(0);
//            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            MsgBox("onPhyUpdate()", "log");
            Log.d(TAG, String.format("txPhy=%d,  rxPhy=%d,  status=%d", txPhy, rxPhy, status));
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            MsgBox("onPhyRead()", "log");
            Log.d(TAG, String.format("txPhy=%d,  rxPhy=%d,  status=%d", txPhy, rxPhy, status));
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            MsgBox("onCharacteristicChanged()", "log");
//            Log.d(TAG, String.format("      UUID:%s, Type:%s", characteristic.getUuid(), characteristic.getWriteType()));
//            Log.d(TAG, String.format("         Value: %s", characteristic.getValue()));
//            Log.d(TAG, String.format("         Value (string): %s", characteristic.getStringValue(0)));
//            Log.d(TAG, String.format("         Property=%d, Permission=%d", characteristic.getProperties(), characteristic.getPermissions()));
            final byte[] data = characteristic.getValue();
//            Log.d(TAG, "DATA length " + data.length);
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%c", byteChar));
                Log.d(TAG,"Read data: " + stringBuilder.toString());
                response.add(stringBuilder.toString());
                isGetResponse = true;
            }
            Log.d(TAG, "------------------------------------------------------------");
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            MsgBox("onDescriptorRead()", "log");
            Log.d(TAG, String.format("      UUID:%s, Permissions:%s", descriptor.getUuid(), descriptor.getPermissions()));
            Log.d(TAG, String.format("         Value: %s", descriptor.getValue()));
            final byte[] data = descriptor.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d(TAG,"Read data: " + stringBuilder.toString());
            }
            BluetoothGattCharacteristic c = descriptor.getCharacteristic();
//            Log.d(TAG, "His characteristic");
//            Log.d(TAG, String.format("      UUID:%s, Type:%s", c.getUuid(), c.getWriteType()));
//            Log.d(TAG, String.format("         Value: %s", c.getValue()));
//            Log.d(TAG, String.format("         Value (string): %s", c.getStringValue(0)));
            // Log.d(TAG, String.format("         Value form descriptor: %s", characteristic.getDescriptor(characteristic.getUuid()).getValue()));
            final byte[] dt = c.getValue();
            if (dt != null && dt.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(dt.length);
                for(byte byteChar : dt)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d(TAG,"Read data: " + stringBuilder.toString());
            }
//            Log.d(TAG, String.format("         Property=%d, Permission=%d", c.getProperties(), c.getPermissions()));
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            MsgBox("onDescriptorWrite()", "log");
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };

    // Конструктор
    public BLEConnection(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        response = new ArrayList<>();
        isConnecting = false;
        isSending = false;
    }

    // подключение к BlueTooth устройству
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean conn(String mac){
        Log.d(TAG, "conn(" + mac + ")");
        isConnecting = true;
        if(!btAdapter.isEnabled()) {
            Log.d(TAG, "BT adapter is off!");
            return false;
        }
        Log.d(TAG, "BT adapter is on");
        if(mac.isEmpty()) {
            Log.d(TAG, "MAC address is empty!");
            return false;
        }
        Log.d(TAG, "MAC: " + mac);
        device = btAdapter.getRemoteDevice(mac);
        if(device == null) {
            Log.d(TAG, mac + " is null");
            return false;
        }
        MsgBox(context.getString(R.string.try_connect) +" "+ device.getAddress(), "all");
        response.add(context.getString(R.string.try_connect) + " " + device.getAddress());
        gatt = device.connectGatt(context, true, btGattCallBack, BluetoothDevice.TRANSPORT_LE);
        return true;
    }

    // подключение к BlueTooth устройству
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void connect(){
        // Достаем из памяти МАС адресс
        mac = preferences.getString(BtConsts.MAC_KEY, "");
        conn(mac);
    }

    public Device getConnectedDevice(){
        if(connectedDevice == null){
            return new Device("Disconnected", "none");
        }
        if(connectedDevice.getName() == null){
            connectedDevice.setName("Disconnected");
        }
        if(connectedDevice.getAddress() == null){
            connectedDevice.setAddress("none");
        }
        return connectedDevice;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean connect_to_saved(String target){
        Log.d(TAG, "\r\nconnect_to_saved()");
//        String target_name = "Elevator_f";
//        if(!target.equals("all")){
//            String f = "";
//            if(target.length() == 1) f ="0";
//            target_name += f + target;
//        }
//        Log.d(TAG, "target_name = " + target_name);
////        if(!btAdapter.isEnabled() || mac.isEmpty() || isConnecting) return;
//        if(!btAdapter.isEnabled()) {
//            Log.d(TAG, "BT adapter is off!");
//            response.add("BT adapter is off!");
//            return false;
//        }
//        if(isConnecting) {
//            Log.d(TAG, "State already in connecting!");
//            response.add("State already in connecting!");
//            return false;
//        }
//        isConnecting = true;
//        Log.d(TAG, "See what to connect...");
//        SetAddressInPreferences storage = new SetAddressInPreferences(context);
//        List<ListItemAddress> cabins = new ArrayList<>();
//        Log.d(TAG, "" + storage.getLength() + " devices may be to connect");
        boolean is_was_connected = false;
//        byte who_was_connected = 0;
//        boolean key = true;
//        for(int i = 0; i < storage.getLength(); i++){
//            ListItemAddress item = storage.getByIndex(i);
//            Log.d(TAG, " - > " + item.getName() + " (" + item.getAddress() + ")");
//            if(item.getName().equals("Cabine")){
//                ListItemAddress tmp = new ListItemAddress();
//                tmp = item;
//                cabins.add(tmp);
////                continue;
//            } else if (item.getName().length() >= target_name.length() && item.getName().substring(0, target_name.length()).equals(target_name)){
//               if(key && conn(item.getAddress())){
//                   boolean res = after_connect(false,"");
//                   Log.d(TAG, "after_connect(false, '') response = " + res);
//                   if(res){
//                       is_was_connected = true;
//                       who_was_connected = item.getFloor();
//                       key = false;
//                   }
//               }
//            }
//        }
//        if(!is_was_connected){
//            Log.d(TAG, "NO ONE WAS CONNECTED TO FLOOR");
//            for(ListItemAddress item : cabins){
//                Log.d(TAG, " -> " + item.getAddress());
//                String command = "lift_";
//                if(target.equals("all")){
//                    command += item.getFloor();
//                    if(connect_cabine(item, command)) is_was_connected = true;
//                    command = "lift_1";
//                    if(connect_cabine(item, command)) is_was_connected = true;
//                } else {
//                    command += target;
//                    if(connect_cabine(item, command)) is_was_connected = true;
//                }
//            }
//        } else {
//            Log.d(TAG, "WAS CONNECTED TO FLOOR");
//            Log.d(TAG, "WAS CONNECTED TO " + who_was_connected);
//            if(target.equals("all")){
//                Log.d(TAG, "target == all");
//                for(ListItemAddress item : cabins){
//                    String cmd_to = "lift_";
////                    String cmd_from = "lift_";
//                    if(item.getFloor() == who_was_connected){
////                        cmd_from += item.getFloor();
//                        cmd_to += "1";
//                    } else {
////                        cmd_from += "1";
//                        cmd_to += item.getFloor();
//                    }
////                    connect_cabine(item, cmd_from);
//                    connect_cabine(item, cmd_to);
//                }
//            }
//        }
//        isConnecting = false;
        return is_was_connected;
    }

    
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean connect_cabine(Device item, String command){
        boolean is_was_connected = false;
        if(conn(item.getAddress())){
            // do something after connect
            boolean res = after_connect(true, command);
            if(res) is_was_connected = true;
            Log.d(TAG, "after_connect(true, " + command + ") response = " + res);
        }
        return is_was_connected;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void disconnect(){
        isConnecting = false;
        if (!btAdapter.isEnabled()) return;
        try {
            if (state_notify) {
                gatt.setCharacteristicNotification(services.get(2).getCharacteristics().get(0), false);
                boolean res = gatt.setCharacteristicNotification(character, false);
                Log.d(TAG, String.format(" Set notification disabled: %s", res));
                state_notify = false;
            }
            gatt.close();
            gatt.disconnect();
            gatt.close();
            gatt = null;
            connect = STATE_DISCONNECTED;
            clearServiceCache();
            connectedDevice = new Device("Disconnected", "none");
            MsgBox(context.getString(R.string.disconnect), "all");
            response.add(context.getString(R.string.disconnect));
        } catch (Exception e){
            Log.d(TAG, e.toString());
        }

    }

    // this function sending command and disconnecting after connect_to_saved()
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean after_connect(boolean isSendCmd, String cmd){
        boolean bg_run = true;
        boolean res = true;
//        Runnable bgRunnable = () -> {
//            int out = 200;
//            while (connect != 2){
//                if (out < 1){
////                    res = false;
//                    return;
//                }
//                sleep(100);
//            }
//            if(isSendCmd){
//                SendMessage(cmd, true);
//            }
//            disconnect();
//        };
//        Thread connThread = new Thread(bgRunnable);
//        connThread.start();

        Log.d(TAG, "Waiting for connect and character ...");
        int out = 10000;
        while ((connect != 2) || (!character_state)){
            if (out-- < 1){
//                    res = false;
                Log.d(TAG, "Connecting OUT OF TIME!");
                return false;
            }
            sleep(1);
        }
        character_state = false;
        if(isSendCmd){
            SendMessage(cmd, true);
            sleep(100);
            SendMessage("getMaxFloors", true);
//            SendMessage("getAddress", true);
            Log.d(TAG, "Waiting for response...");
            int OUT = 10000;
            while (!isGetResponse){
                if(OUT-- < 0){
                    Log.d(TAG, "Wait response is OUT OF TIME");
                    break;
                }
                sleep(1);
            }
            isGetResponse = false;
        }
        sleep(100);
        disconnect();

        return res;
    }

    private void sleep(int ms){
        try{
            Thread.sleep(ms);
        } catch (Exception e){
            Log.d("MainLog", "" + e);
        }
    }


//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void SendMessage(String message, boolean end_of_string){
        Log.d(TAG, "SendMessage(" + message + ")");
        response.add("SendMessage(" + message + ")");
        if(connect == STATE_CONNECTED) { // ==2
            if(character != null) {
                String l = String.format("ch.getProperties() = %d", character.getProperties());
                Log.d(TAG, l);
                response.add(l);
                isSending = true;
//            if(ch.getProperties() == PROPERTY_READ) {
//                gatt.readCharacteristic(ch);
//            } else if(ch.getProperties() == PROPERTY_WRITE) {
//                ch.setValue("_test_");
//                ch.setWriteType(WRITE_TYPE_DEFAULT);
//                gatt.writeCharacteristic(ch);
//            } else if(ch.getProperties() == PROPERTY_NOTIFY){
////                gatt.setCharacteristicNotification(ch, true);
//                gatt.readCharacteristic(ch);
//            } else if(ch.getProperties() == 30){
//                gatt.readCharacteristic(ch);
////                ch.setValue("_test30p_");
////                ch.setWriteType(WRITE_TYPE_DEFAULT);
////                gatt.writeCharacteristic(ch);
//            } else if(ch.getProperties() == 32){
//                gatt.readCharacteristic(ch);
//            }
                if(end_of_string){
                    character.setValue(message + "/-");
                }
                character.setWriteType(WRITE_TYPE_DEFAULT);
                gatt.writeCharacteristic(character);
                Log.d(TAG, "Sended!");
                response.add("Sended!");
                if (!state_notify) {
                    boolean res = gatt.setCharacteristicNotification(character, true);
                    Log.d(TAG, String.format(" Set notification enabled: %s", res));
                    state_notify = true;
                }
                isSending = false;
//            List<BluetoothGattDescriptor> descriptors = ch.getDescriptors();
//            for(BluetoothGattDescriptor descriptor : descriptors){
//                gatt.readDescriptor(descriptor);
//            }
            }
        }

        Log.d(TAG, "");
    }

    private boolean clearServiceCache(){
        boolean result = false;
        try{
            Method refreshMethod = gatt.getClass().getMethod("refresh");
            if(refreshMethod != null){
                result = (boolean) refreshMethod.invoke(gatt);
            }
        } catch (Exception e){
            Log.e(TAG, "ERROR: Could not invoke refresh method");
        }
        return result;
    }

    private void MsgBox(String message, String n){
        if(n.equals("log") || n.equals("all")) Log.d(TAG, message);
        if(n.equals("toast") || n.equals("all")) {
            Toast toast = Toast.makeText(this.context, message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 10, 20);
            toast.show();
        }
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        return null;
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        return null;
    }

    @SuppressLint("WrongConstant")
    @Override
    public int getConnectionState(BluetoothDevice device) {
        return 0;
    }

    public int getConnectState(){
        return connect;
    }

    public boolean isSendingMessage(){ return isSending; }

    public int isHaveData(){
        if(response != null) return response.size();
        return 0;
    }
}


