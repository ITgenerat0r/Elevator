package com.example.elevator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.elevator.adapter.BtConsts;
import com.example.elevator.adapter.BtnItem;
import com.example.elevator.adapter.ButtonAdapter;
import com.example.elevator.adapter.LiftAdapter;
import com.example.elevator.bluetoothle.BLEConnection;
import com.example.elevator.objects.Control;
import com.example.elevator.objects.Device;
import com.example.elevator.objects.Elevator;
import com.example.elevator.objects.Storage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    boolean developer_mode = false;
    private MenuItem menuItem;
    private MenuItem menu_list_devices;
    private MenuItem menu_bt_connect;
    private MenuItem menu_test;
    private MenuItem menu_settings;
    private BluetoothAdapter btAdapter;
    private final int ENABLE_REQUEST = 15;
    private final static String TAG = "MainActivity";
    private Button btn_send; // Button for send message to HMSoft
    private Button btn_clear; // Button for clear log (dialog_history)
    private long dev_switch_delay = 0;
    private int dev_switch_count = 0;
    private EditText input_text; // Field for input commands
    private TextView current_address;
    private TextView current_floor;
    private TextView dialog_history; // поле для вывода информации(Отправленной и полученной, а так же debugs)
    private CheckBox end_of_send;
    private boolean bluetooth_status; // Power_button bluetooth
    private boolean connect_button_state; // status Bluetooth connecting button
    private SharedPreferences preferences; // value for save information in storage
    private BLEConnection btConnection;
    

    private Handler handler;
    private List<String> log;

    private GridView gridView;
    private List<BtnItem> list_btn; // Список объектов кнопок лифта
    private ButtonAdapter btnAdapter; // Адаптер для кнопок лифта
    private LiftAdapter liftAdapter;
    private byte max_floors = 2; // количество этажей
    private Device connectedDevice = null;
    private List<String> listFloors;
    private boolean background_loop = true;


    private long start; // Время начала отправки данных на BLE модуль, в миллисекундах (для тестирования времени ответа от устройства)


    private byte currentFloor; //
    private String currentAddress; // Current elevator ID
    private String homeAddress; // Home elevator ID
    private int my_floor = 0;
    private boolean auto_move_me = false;




    // For auto discovering
    private boolean background_auto = true;
    private boolean background_auto_connect = false;
    private boolean background_auto_discover = true;
    private byte position = 0; // Номер нажатой кнопки, хранится здесь для команды вызова и др.
    private boolean call_when_discover_end = false;
    private boolean auto_call_when_discover_end = false;
    private boolean done = false; // for connect in background
    private boolean isBtConnected = false;
    private final int OVER = 2000;
    private final int BT_REQUEST_PERMISSION = 111; // Code for broadcasteceiver
    private static final int MY_CAMERA_REQUEST_CODE = 115;
    private final byte COUNT_DISCOVERS = 1;  // Количество попыток обнаружить устройства
    private final byte UPDATE_LIST_AFTER = 3;
    private byte count_for_update_list = 0;
    private boolean isBtPermissionGranted = false; // Разрешение геолокации
    private boolean isCameraPermissionGranted = false; // Разрешение доступа к камере
    private boolean isBtDiscovering = false; // State discovering
    private byte countDiscover = 0; // Количество обнаружений
    private boolean background_wait_for_disconnect = false;
    private boolean delay_for_disconnect = false;
    private boolean background_send_command = false;
    private List<DiscoveredDevice> listDiscoveredDevices = new ArrayList<>(); // Хранит информацию об обнаруженных устройствах
    private boolean access_to_list_discovered_devices = false; // true если идет цикл по listDiscoveredDevices,
                                                                // нужен что бы не произошло одновременного обращения к списку
    private boolean wrong_address = false; // если при автоматическом режиме не нашлось адресов сохраненных в памяти, то будет равно true
    //
    private List<Elevator> listSavedElevators;

    private int debug_int = 200; // for change int parameters from app


    static class DiscoveredDevice {
        String Name;
        String Address;
        List<Integer> rssi;
    }



    private boolean test_state = false; // Состояние кнопки тестирования
    @SuppressLint("HandlerLeak")
    private Handler bgHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
//            Log.d(TAG, "bgHandler()");
            super.handleMessage(msg);
            Bundle bndl = msg.getData();
            ArrayList<String> data_response = bndl.getStringArrayList("MSG_LIST_KEY");
            // Выполняем нужное действие если bgHandler отправил sendMessage()
//            Log.d(TAG, "Size data_response: " + data_response.size());
            for(String i : data_response){
//                Log.d(TAG, "Getting data: " + i);
                dialog_history.append(i + "\r\n");
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler autoHandler = new Handler(){
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bndl = msg.getData();
            String command = bndl.getString("MSG_COMMAND");
            Log.d(TAG, "command: " + command);
            if(command == null){
                Log.d(TAG, "command is null");
            } else if(command.equals("discover")){
                // discover devices
                if(isBtPermissionGranted && btAdapter.isEnabled()){
                    if(!isBtDiscovering){
                        Log.d(TAG, "Start discovering...");
                        isBtDiscovering = true;
                        btAdapter.startDiscovery();
                    }
                }
            } else if (command.equals("connect")){
                if(!connect_button_state){
//                    btConnection.connect();
//                    connect_button_state = true;
                    startConnect();
//                    menu_bt_connect.setIcon(R.drawable.ic_connected);
                }
            } else if (command.substring(0, 5).equals("conn_")){
                btConnection.conn(command.substring(5));
            } else if (command.equals("connect_to_saved")) {
                btConnection.connect_to_saved("all");
            } else if (command.substring(0, 4).equals("gUpd")){
                btConnection.disconnect(); // for debug, chande for release
            }else if (command.equals("disconnect")){
                btConnection.disconnect();
            } else {
                if(isBtConnected){
                    Log.d(TAG, "Send message: " + command);
                    btConnection.SendMessage(command, true);
                }
            }
            Log.d(TAG, "end autoHandler (msg)");
        }
    };

    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startConnect(){
        done = false;
//        background_loop = false;
        BLEConnection btCon = new BLEConnection(this);
        if(btAdapter.isEnabled() && btCon != null){
            btCon.connect();
        }

        // bring from autobackground
        // Get address and maxfloors from device
        Log.d(TAG, "Wait for connect...");
        for (int over = 0; over < 600; over++){
            sleep(100);
            if(btCon.getConnectState() == 2){
                Log.d(TAG, "AutoConnected");
                break;
            }
        }
        Log.d(TAG, "Reading...");
        currentAddress = "Reading...";
//        if(true) return;
        sleep(2500);
        if(btCon.getConnectState() == 2){
            Log.d(TAG, "Connected (auto)");
            Log.d(TAG, "Send msg for address");
            btCon.SendMessage("getAddress", true);
        } else {
            Log.d(TAG, "Connection failed");
            return;
        }
        for(int over = 0; over < OVER; over++){
            isBtConnected = (btCon.getConnectState() == 2);
            readResponse(btCon);
            if(!currentAddress.equals("Reading...")){
                break;
            }
            sleep(100);
        }

        sleep(2500);
        max_floors = -1;
        if(isBtConnected){
            Log.d(TAG, "Send msg for max floors");
            btCon.SendMessage("getMaxFloors", true);
        }
        for(int over = 0; over < OVER; over++){
            isBtConnected = (btCon.getConnectState() == 2);
            readResponse(btCon);
            if(max_floors != -1){
                break;
            }
            sleep(100);
        }

        // Send command to device
        if(auto_move_me && homeAddress.equals(currentAddress) && (currentFloor == 1 || currentFloor == my_floor)){
            int from = currentFloor;
            int to = my_floor;
            if (currentFloor == my_floor){
                to = 1;
            }
            Log.d(TAG, "Send command");
            btCon.SendMessage(String.format("liftto_%d_%d", from, to), true);
        }
        sleep(2500);
        btCon.disconnect();
        sleep(2500);
        for(int over = 0; over < OVER; over++){
            if(btCon.getConnectState() == 0)break;
        }
        btCon = null;
        Log.d(TAG, "End startConnect()");
//        backgroundResponse();
        done = true;
    };

    @SuppressLint("HandlerLeak")
    private Handler responseHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            isBtConnected = (btConnection.getConnectState() == 2);
            if(isBtConnected){
                menuItem.setIcon(R.drawable.ic_bt_connected);
            } else {
                setBtIcon();
            }
            connectedDevice = btConnection.getConnectedDevice();
            current_address.setText("");
            readResponse(btConnection);
            if(currentFloor > 0){
//                current_floor.setText("" + currentFloor);
            }
        }
    };

    private void readResponse(BLEConnection con){
//        Log.d(TAG, "readResponse()");
        if(con != null && con.isHaveData() > 0){
//            Log.d(TAG, "con.getResponse()");
//            dialog_history.append("con.getResponse();\r\n");
            List<String> data_response = con.getResponse();
//            btConnection.clearResponse();
            for(String i : data_response){
                dialog_history.append(i + "\r\n");
                if(i.length() > 3){
                    if(i.substring(0,3).equals("flr")){
//                        String  g = i.substring(3);
                        try {
                            max_floors = Byte.parseByte(i.substring(3));
                            Log.d(TAG, "getMaxFloors received value == " + max_floors);
                            UpdateMaxFloors();
                        } catch (Exception e){
                            Log.e(TAG, "g is have wrong type (not byte)");
                        }
                    } else if (i.substring(0,4).equals("addr")){
                        if(i.length() > 4){
                            String address = i.substring(4);
                            Log.d(TAG, "Elevator ID: " + address);
                            Log.d(TAG, "getAddress received value == " + address);
                            currentAddress = address;
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(BtConsts.CURRENT_ADDRESS, currentAddress);
                            editor.apply();
                            current_address.setText("Current address: " + currentAddress);
                        }
                    } else if (i.equals("speed")){
                        DateFormat tm = new SimpleDateFormat("KK:mm:ss", Locale.getDefault());
                        String time = tm.format(new Date());
                        Log.d(TAG, "Speed <- RX");
                        dialog_history.append("Speed <- RX   [" + time + "]\r\n");
                        dialog_history.append(" Executed for " + (System.currentTimeMillis() - start) + " milliseconds.\r\n");
                    }
                }
            }
            con.clearResponse();
        }
    }

    private void UpdateMaxFloors() {
        if (max_floors < 1) return;
        while (listFloors.size() != max_floors){
            if(listFloors.size() < max_floors){
                listFloors.add("");
            } else {
                listFloors.remove(listFloors.size() - 1);
            }
        }
        liftAdapter.notifyDataSetChanged();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "\n\r.\n\r.\n\r.\n\r.\n\r.\n\r.\n\r");
        Log.d(TAG, "Start program... (onCreate)");
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        init();
        getBtPermission();
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void init(){
        Log.d(TAG, String.format("init %s", getString(R.string.app_name)));
        connect_button_state = false;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btn_send = findViewById(R.id.bt_send);
        btn_send.setOnClickListener(this::onClick);
        btn_clear = findViewById(R.id.button_clear);
        btn_clear.setOnClickListener(this::onClick);

        input_text = findViewById(R.id.input_text);
        current_address = findViewById(R.id.address);
        current_floor = findViewById(R.id.floor);
        dialog_history = findViewById(R.id.dialog_history);
        end_of_send = findViewById(R.id.checkBox_at);
        preferences = getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
        Log.d(TAG, "MAC in preferences: " + preferences.getString(BtConsts.MAC_KEY, "none"));
        homeAddress = preferences.getString(BtConsts.MY_ADDRESS, "none");
        my_floor = preferences.getInt(BtConsts.MY_FLOOR, 0);
        auto_move_me = preferences.getBoolean(BtConsts.LIFT_ME, false);
        Toast toast = Toast.makeText( this , "MAC in preferences: " + preferences.getString(BtConsts.MAC_KEY, "none"), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 10 , 20);
        toast.show();
        btConnection = new BLEConnection(this);

        dev_switch_delay = System.currentTimeMillis();
        dev_switch_count = 0;

        list_btn = new ArrayList<BtnItem>();
        gridView = findViewById(R.id.list_buttons);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
//            Toast.makeText(getApplicationContext(), "Clicked " + (position + 1), Toast.LENGTH_SHORT).show();
            Call(position + 1);
            // Test for speed
//            if(btConnection != null && btConnection.getConnectState() == 2){
//                DateFormat tm = new SimpleDateFormat("KK:mm:ss", Locale.getDefault());
//                String time = tm.format(new Date());
//                Log.d(TAG, "Speed -> TX");
//                dialog_history.append("Speed -> TX   [" + time + "]\r\n");
//                start = System.currentTimeMillis();
//                btConnection.SendMessage("speed", true);
////                btConnection.SendMessage("call_" + (position + 1));
//            }
        });


        btn_clear.setText(String.format("%s", "DEV"));
        btn_send.setVisibility(View.GONE);
        end_of_send.setVisibility(View.GONE);
        input_text.setVisibility(View.GONE);

        listFloors = new ArrayList<String>();
        for(int i = 0; i < max_floors; i++){
            listFloors.add("0");
        }


        liftAdapter = new LiftAdapter(MainActivity.this, listFloors);
        gridView.setAdapter(liftAdapter);

        backgroundAuto();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("DefaultLocale")
    private void Call(int pos){
        Log.d(TAG, String.format("Call(%d)", pos));
        dialog_history.append(String.format("Call(%d)\r\n", pos));
        if (!btAdapter.isEnabled()){
            Log.d(TAG,"BT is off, Call() aborted");
            dialog_history.append("BT is off, Call() aborted\r\n");
            return;
        }
        // if already connected
        if (btConnection.getConnectState() == 2){
            Log.d(TAG, "BT is already connected!");
            btConnection.SendMessage(String.format("lift_%d", pos), true);
//            for(Object dvc : btConnection.getConnectedDevices()){
//                Log.d(TAG, "checkpoint");
//                String name = dvc.getClass().getName();
//                Log.d(TAG, String.format("Already connected to: " + name));
//                dialog_history.append("Already connected to: " + name + "\r\n");
//                if(name.equals("Cabine")){
//                    btConnection.SendMessage(String.format("lift_%d", pos), true);
//                }
//            }
            return;
        }

        Log.d(TAG, "BT isn't connected!");

        // check in saved elevators
        String ename = "Elevator_f" + pos;
        for(Elevator elv : listSavedElevators){
            for(Device dvc : elv.getFloors()){
                Log.d(TAG, "ADDRESS: " + dvc.getAddress() + ", NAME: " + dvc.getName());
                if(dvc.getName().equals(ename) && btConnection.getConnectState() == 0) btConnection.conn(dvc.getAddress()); //  && !btConnection.isConnecting() - there was in conditions
            }
            for(Device dvc : elv.getCabins()){
                Log.d(TAG, "ADDRESS: " + dvc.getAddress() + ", NAME: " + dvc.getName());
                if(btConnection.getConnectState() == 0) btConnection.conn(dvc.getAddress());
            }
        }
        Log.d(TAG, String.format("Connected state: %d", btConnection.getConnectState()));
//        dialog_history.append(String.format("Connected state: %d", btConnection.getConnectState()) + "\r\n");

        // wait for connect
        background_wait_for_disconnect = true;
        position = Byte.parseByte(String.valueOf(pos));

//        if (btConnection.getConnectState() == 2){
//            for(Object dvc : btConnection.getConnectedDevices()){
//                String name = dvc.getClass().getName();
//                Log.d(TAG, String.format("Already connected to: "+ name));
//                dialog_history.append("Already connected to: "+ name);
//                if(name.equals("Cabine")){
//                    btConnection.SendMessage(String.format("lift_%d", pos), true);
//                }
//            }
//            return;
//        }

//        if(pos > -100) return;
//        position = Byte.parseByte("" + pos);
//        current_address.setText(String.format("Идет вызов на %d этаж...", pos));
//        Log.d(TAG, String.format("Идет вызов на %d этаж...", pos));
//        if(btConnection.getConnectState()==2){
//            Log.d(TAG, "BT is connected");
//            background_wait_for_disconnect = true;
////            List<BluetoothDevice> t = btConnection.getConnectedDevices();
////            Log.d(TAG, "Connected devices was received.");
////            for(BluetoothDevice i : t){
////                Log.d(TAG,"Name: "+i.getName());
////            }
////            Log.d(TAG, "_|");
////            BluetoothDevice d = t.get(0);
////            Log.d(TAG, "First connected device is received");
//            if(true){ // d.getName().equals("Cabine")
//                Log.d(TAG, "BT is Cabine");
//                background_send_command = true;
//            } else {
//                Log.d(TAG, "BT is Floor");
//                background_send_command = false;
//            }
//            return;
//        }
//        if(isBtDiscovering){
//            Log.d(TAG, "BT is discovering");
//            call_when_discover_end = true;
//            return;
//        }
//        String fl = "Elevator_f";
//        if(pos < 10) fl += "0";
//        fl += pos;
//        Log.d(TAG, "fl = " + fl);
//        if(!access_to_list_discovered_devices){
//            Log.d(TAG, "We have access to list of discovered devices");
//            access_to_list_discovered_devices = true;
//            for (DiscoveredDevice dvc : listDiscoveredDevices) {
//                Log.d(TAG, "-> " + dvc.Name + " " + dvc.Address);
//                if(dvc.Name.equals("Cabine")){
//                    Log.d(TAG, "find 'Cabine'");
//                    if(btConnection.getConnectState() == 0 && !btConnection.isConnecting()) btConnection.connect();
//                    background_wait_for_disconnect = true;
//                    background_send_command = true;
//                    break;
//                } else if(dvc.Name.equals(fl)){
//                    Log.d(TAG, "find '" + fl + "'");
//                    background_wait_for_disconnect = true;
//                    background_send_command = false;
//                    break;
//                }
//            }
//            Log.d(TAG, "find None");
//            access_to_list_discovered_devices = false;
//        }


    }

    // not use
//   Вызов лифта с автоматическим поиском и определением блютуз модуля
    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void Call_auto(int pos){

        Log.d(TAG, "      ->  Call_auto();");
        position = Byte.parseByte("" + pos);
        current_address.setText(String.format("Идет вызов на %d этаж...", pos));
        Log.d(TAG, String.format("Идет вызов на %d этаж...", pos));

        if(isBtDiscovering){
            auto_call_when_discover_end = true;
            return;
        }
        Log.d(TAG, "      ->  step");
        DiscoveredDevice call_dvc = new DiscoveredDevice();
        Log.d(TAG, "      ->  step1");
        call_dvc.Name = "Empty";
        call_dvc.Address = "None";
        Log.d(TAG, "      ->  step2");
        call_dvc.rssi = new ArrayList<Integer>();
        Log.d(TAG, "      ->  step3");
        call_dvc.rssi.add(1000);
        Log.d(TAG, "      ->  step4");
        boolean isCabine = false;
        Log.d(TAG, "      ->  step5");
        String nm = "Elevator_f";
        if(pos < 10){
            nm += "0";
        }
        nm += pos;

        if(!access_to_list_discovered_devices) {
            access_to_list_discovered_devices = true;
            for (DiscoveredDevice dvc : listDiscoveredDevices) {
                Log.d(TAG, "      ->  ->  step - " + dvc.Name);
                if (dvc.Name.equals(nm)) {
                    Log.d(TAG, "      ->  ->  step1true");
                    if (call_dvc.Name.equals("Cabine")) {
                        Log.d(TAG, "      ->  ->  step2true");
                        if (dvc.rssi.get(0) < call_dvc.rssi.get(0)) {
                            Log.d(TAG, "      ->  ->  step3true");
                            call_dvc = dvc;
                            isCabine = false;
                            break;
                        }
                    } else {
                        Log.d(TAG, "      ->  ->  step2false");
                        call_dvc = dvc;
                        isCabine = false;
                    }
                } else if (dvc.Name.equals("Cabine")) {
                    Log.d(TAG, "      ->  ->  step1false");
                    if (call_dvc.Name.equals("Empty")) {
                        Log.d(TAG, "      ->  ->  step2true");
                        call_dvc = dvc;
                        isCabine = true;
                    } else {
                        Log.d(TAG, "      ->  ->  step2false");
                        if (call_dvc.rssi.get(0) >= dvc.rssi.get(0)) {
                            Log.d(TAG, "      ->  ->  step3true");
                            call_dvc = dvc;
                            isCabine = true;
                        }
                    }
                }
            }
            access_to_list_discovered_devices = false;
        } else {
            String.format("No access, BT is busy");
            auto_call_when_discover_end = true;
            return;
        }

        Log.d(TAG, "      ->  step6");
            if (!call_dvc.Name.equals("Empty")) {
                // set in preferences dvc.Address
                Log.d(TAG, " -> set in preferences dvc.Address " + call_dvc.Address + " from Name: " + call_dvc.Name);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(BtConsts.MAC_KEY, call_dvc.Address);
                editor.putString(BtConsts.LAST_NAME, call_dvc.Name);
                editor.apply();
                // connect
                Log.d(TAG, " -> Connect");
                if(btConnection.getConnectState() == 0 && !btConnection.isConnecting()) btConnection.connect();
                // wait and disconnect moved on background auto
                background_wait_for_disconnect = true;
                if(isCabine){
                    background_send_command = true;
                } else {
                    background_send_command = false;
                }
                position = Byte.parseByte("" + pos);
            }

        Log.d(TAG, "      ->  step7");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menuItem = menu.findItem(R.id.id_bt_button);
        menu_list_devices = menu.findItem(R.id.id_menu);
        menu_bt_connect = menu.findItem(R.id.id_bt_connect);
        menu_test = menu.findItem(R.id.id_bt_test);
        menu_settings = menu.findItem(R.id.id_settings);

        setBtIcon();
        setDeveloperMode();

        backgroundResponse();

        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        homeAddress = preferences.getString(BtConsts.MY_ADDRESS, "none");
        my_floor = preferences.getInt(BtConsts.MY_FLOOR, 0);
        auto_move_me = preferences.getBoolean(BtConsts.LIFT_ME, false);

//        Log.d(TAG, "OnStart()");

        Storage storage = new Storage(this);
        listSavedElevators = storage.getStorage();
        super.onStart();

    }

    @SuppressLint("ResourceType")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // кнопка power bluetooth
        if(item.getItemId() == R.id.id_bt_button){
            if(!btAdapter.isEnabled()){
                enableBt();
            } else {
                btAdapter.disable();
                menuItem.setIcon(R.drawable.ic_bt_disabled);
                menuItem.setTitle(getString(R.string.enable_BT));
                menu_list_devices.setEnabled(false);
                menu_bt_connect.setEnabled(false);
                bluetooth_status = false;
            }
            // кнопка переход в список устройств
        } else if(item.getItemId() == R.id.id_menu){
            if(btAdapter.isEnabled()){
                Intent i = new Intent(MainActivity.this, BtListActivity.class);
                startActivity(i);
            }else{
                Toast.makeText(this, "Bluetooth is off", Toast.LENGTH_SHORT).show();
                setBtIcon();
            }
            // кнопка connect
        } else if (item.getItemId() == R.id.id_bt_connect){
            if(!connect_button_state){
                btConnection.connect();
                connect_button_state = true;
                menu_bt_connect.setIcon(R.drawable.ic_connected);
            } else {
                btConnection.disconnect();
                connect_button_state = false;
                menu_bt_connect.setIcon(R.drawable.ic_connection);
            }
        } else if (item.getItemId() == R.id.id_settings) {
            getCameraPermission();
            setBtIcon();
            if(isCameraPermissionGranted) {
//                if (!bluetooth_status) {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                    builder.setTitle("Bluetooth выключен");
//                    builder.setMessage("Вклчить bluetooth?");
//                    builder.setPositiveButton("Включить", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            ((MainActivity) getActivity()).enableBt();
//                        }
//                    });
//                    builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            // Do something if pressed button "Cancel"
//                        }
//                    });
//
//                    AlertDialog dialog = builder.create();
//                    dialog.show();
//                }
//                if (bluetooth_status) {
                    Intent settings_activity = new Intent(MainActivity.this, ListElevatorsActivity.class);
                    startActivity(settings_activity);
//                }
            }

        } else if (item.getItemId() == R.id.id_bt_test){
            if(!test_state){
                menu_test.setIcon(R.drawable.ic_test_on);
                test_state = true;
                if(!connect_button_state) {
//                    menu_bt_connect.setEnabled(false);

                    background_auto_discover = true;
                    backgroundAutoConnect();
//                    backgroundAuto();
                }
            } else {
                menu_test.setIcon(R.drawable.ic_test_off);
                test_state = false;
                background_auto_discover = false;
                background_auto_connect = false;
//                background_auto = false;

            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void ch(){
        Control cntr = new Control();
        if(!cntr.check()){
            Toast toast = Toast.makeText( this , R.string.update_app, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 10 , 40);
            toast.show();
            sleep(10000);
            this.finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ENABLE_REQUEST){
            if(resultCode == RESULT_OK){
                // Do something if all is good
                setBtIcon();
            }
        }
    }

    private void setBtIcon(){
        if(btAdapter.isEnabled()){
            menuItem.setIcon(R.drawable.ic_bt_enabled);
            menuItem.setTitle(getString(R.string.disable_BT));
            menu_list_devices.setEnabled(true);
            menu_bt_connect.setEnabled(true);
            bluetooth_status = true;
        } else {
            menuItem.setIcon(R.drawable.ic_bt_disabled);
            menuItem.setTitle(getString(R.string.enable_BT));
            menu_list_devices.setEnabled(false);
            menu_bt_connect.setEnabled(false);
            bluetooth_status = false;
        }
    }

    private void setDeveloperMode(){
        Log.d(TAG, String.format("setDeveloperMode to: %b", developer_mode));
        menu_test.setVisible(developer_mode);
        menu_list_devices.setVisible(developer_mode);
        menu_bt_connect.setVisible(developer_mode);
        input_text.setText("");
        if(developer_mode){
            btn_clear.setText(String.format("%s", "Clear"));
            btn_send.setVisibility(View.VISIBLE);
            end_of_send.setVisibility(View.VISIBLE);
            input_text.setVisibility(View.VISIBLE);
        } else {
            btn_clear.setText(String.format("%s", "DEV"));
            btn_send.setVisibility(View.GONE);
            end_of_send.setVisibility(View.GONE);
            input_text.setVisibility(View.GONE);
        }
//        Log.d(TAG, "done!");
    }


    private void enableBt(){
        Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(i, ENABLE_REQUEST);
        setBtIcon();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_send:
                String text = input_text.getText().toString();
                if(text.equals("clear")){
                    dialog_history.setText("");
                    input_text.setText("");
                    return;
                } else if (text.equals("exit DEV") || text.equals("qdev")){
                    developer_mode = false;
                    setDeveloperMode();
                }else if (text.length() > 9){
                    if(text.substring(0, 9).equals("debug_int")){
                        debug_int = Integer.parseInt(text.substring(10));
                    }
                } else if (text.equals("help")){
                    Log.d(TAG, "help");
                    dialog_history.append("Commands\r\n");
                    dialog_history.append(" lift_X (X >= 1)\r\n");
                    dialog_history.append(" liftto_X_Y (X >= 1, Y >= 1)\r\n");
                    dialog_history.append(" getAddress\r\n");
                    dialog_history.append(" getMaxFloors\r\n");
                    dialog_history.append(" speed\r\n");
                    dialog_history.append(" gExtBtns  // get extra buttons\r\n");
                    dialog_history.append(" gUpd  // get update code\r\n");
                    dialog_history.append(" pExtBtn_X (1 <= X <= 4)  // press extra button\r\n");
                    input_text.setText("");
                }

                if(btConnection != null && btConnection.getConnectState() == 2){
                    if(text.equals("disconnect")){
                        btConnection.disconnect();
                    } else {
                        btConnection.SendMessage(text, !end_of_send.isChecked());
                    }
                } else {
                    return;
                }
                dialog_history.append(input_text.getText() + "\r\n");
                //input_text.notify();
                Toast toast = Toast.makeText(this, "Send text: " + input_text.getText(), Toast.LENGTH_LONG);
                input_text.setText("");
                toast.setGravity(Gravity.TOP, 10, 20);
                toast.show();
                break;
            case R.id.button_clear:
                dialog_history.setText("");
                Log.d(TAG, String.format("time between press button Clear: %d - %d = %d", System.currentTimeMillis(), dev_switch_delay, System.currentTimeMillis() - dev_switch_delay));
                if(System.currentTimeMillis() - dev_switch_delay > 500){
                    dev_switch_delay = System.currentTimeMillis();
                    dev_switch_count = 0;
                    break;
                }
                dev_switch_delay = System.currentTimeMillis();
                dev_switch_count += 1;
                if(dev_switch_count < 2) break;
                developer_mode = !developer_mode;
                setDeveloperMode();
            default:
                break;
        }
    }

    private Object getActivity() {
        return this;
    }

    private void backgroundResponse(){
        ////////// BACKGROUND RESPONSE /////////////////////
        Log.d(TAG, "Start backgroundResponse");
        background_loop = true;
        Runnable bgRunnable = () -> {
            List<String> response_data = new ArrayList<>();
//            response_data.add("Test background");
            while(background_loop){
//                        Log.d(TAG, "Loop in background");
                if(response_data.size() > 0){
                    Log.d(TAG, "We have data!");
                    ArrayList<String> msg_data = new ArrayList<>();
                    for(String i : response_data){
                        msg_data.add(i);
                        Log.d(TAG, "Added data: " + i);
                    }
                    Message msg = bgHandler.obtainMessage();
                    Bundle bndl = new Bundle();
//                            try {
//                                Thread.sleep(5000);
//                            } catch (Exception e){
//                                e.printStackTrace();
//                            }
                    response_data.clear();
                    bndl.putStringArrayList("MSG_LIST_KEY" , msg_data);
                    msg.setData(bndl);
                    bgHandler.sendMessage(msg);
                }
                responseHandler.sendEmptyMessage(0);
                try {
                    Thread.sleep(50);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            // show message: end test background
        };
        Thread bgThread = new Thread(bgRunnable);
        bgThread.start();

        //////////// BACKGROUND RESPONSE END /////////////////
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void backgroundAuto(){
        Log.d(TAG, "start backgroundAuto()");
        if(!btAdapter.isEnabled())return;
        background_auto = true;
        boolean background_auto_version_2 = false;
        background_auto_discover = false;
        @SuppressLint("DefaultLocale") Runnable autoRunnable = () -> {
            while (background_auto){
                if(background_wait_for_disconnect){
                    Log.d(TAG, " -> Wait for connect...");
                    while (!isBtConnected){
                        sleep(100);
                    }
                    Log.d(TAG, "Waiting done.");

                    if(connectedDevice.getName().equals("Cabine")){
                        // Отправляем лифт на нужный этаж
                        Log.d(TAG, " -> autoHandler command " + String.format("lift_%d", position));
                        Message msg = autoHandler.obtainMessage();
                        Bundle bndl = new Bundle();
                        bndl.putString("MSG_COMMAND", String.format("lift_%d", position));
                        msg.setData(bndl);
                        autoHandler.sendMessage(msg);
                        sleep(250);

//                        sleep(debug_int); // only for debug, delete or change to static value for release
                        // Проверяем наличие изменений
//                        Log.d(TAG, " -> autoHandler command gUpd");
//                        msg = autoHandler.obtainMessage();
//                        bndl = new Bundle();
//                        bndl.putString("MSG_COMMAND", "gUpd");
//                        msg.setData(bndl);
//                        autoHandler.sendMessage(msg);
//                        sleep(100);

                        delay_for_disconnect = true; // this for debug
                        while (delay_for_disconnect){
                            sleep(100);
                            // disconnect
                            Log.d(TAG, " -> Disconnect");
                            msg = autoHandler.obtainMessage();
                            bndl = new Bundle();
                            bndl.putString("MSG_COMMAND", "disconnect");
                            msg.setData(bndl);
                            autoHandler.sendMessage(msg);
                        }

//                        sleep(debug_int); // only for debug, change to static value for release
                    } else {
                        sleep(1000);
                        // disconnect
                        Log.d(TAG, " -> Disconnect");
                        Message msg = autoHandler.obtainMessage();
                        Bundle bndl = new Bundle();
                        bndl.putString("MSG_COMMAND", "disconnect");
                        msg.setData(bndl);
                        autoHandler.sendMessage(msg);
                    }
                    sleep(100);

                    background_wait_for_disconnect = false;
                }
//                try{
//                    Log.d(TAG, "background sleep");
//                    Thread.sleep(90 * 1000);
//                } catch (Exception e){
//                    Log.d(TAG, e.toString());
//                }

            }
            while (background_auto_version_2) {
                Log.d(TAG, "Next in backgroundAuto");
                countDiscover = 0;
                // Очистка массива listDiscoveredDevices для обновления данных
                if(count_for_update_list >= UPDATE_LIST_AFTER){
                    listDiscoveredDevices.clear();
                }
                while ((countDiscover < COUNT_DISCOVERS) && background_auto_discover) {
                    if (!isBtDiscovering) {
                        Log.d(TAG, "  Send msg for discovering");
                        Message msg = autoHandler.obtainMessage();
                        Bundle bndl = new Bundle();
                        bndl.putString("MSG_COMMAND", "discover");
                        msg.setData(bndl);
                        autoHandler.sendMessage(msg);
                        while (!isBtDiscovering){
                            sleep(200);
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        Log.d(TAG, "  not sleep");
                    }
                }
                count_for_update_list++;
                Log.d(TAG, "  All discovers vere done!");
                // Print list of discovered devices
                String currentAddress = "";
                String currentName = "";
                // Определение близжайшего устройсва по данным сканирвания
                int currentRSSI = 1000;
                Log.d(TAG, "Loop in discovered devices");

                // Ждем пока список устройств освободится от других процессов
                while (access_to_list_discovered_devices){
                    sleep(200);
                }
                access_to_list_discovered_devices = true;
                for (DiscoveredDevice dvc : listDiscoveredDevices) {
                    String RSSIs = "   ";
                    int tmp = 0;
                    int count = 0;
                    for (int a : dvc.rssi) {
                        RSSIs += a + "   ";
                        count++;
                        tmp += a;
                    }
//                    float middle = tmp / count; // среднее арифметическое
                    Log.d(TAG, "Name " + dvc.Name + " (" + dvc.Address + "):" + RSSIs);
                    Log.d(TAG, String.format("count: %d", count));
                    if (tmp < currentRSSI && count == COUNT_DISCOVERS && dvc.Name != null && dvc.Name.length() > 8 && dvc.Name.substring(0, 8).equals("Elevator")) {
                        Log.d(TAG, "Set this device\r\n");
                        currentAddress = dvc.Address;
                        currentName = dvc.Name;
                    }
                }
                access_to_list_discovered_devices = false;

                Log.d(TAG, "End loop");

                if(background_wait_for_disconnect){
//                    {
//                        Log.d(TAG, " -> Connecting...");
//                        Message msg = autoHandler.obtainMessage();
//                        Bundle bndl = new Bundle();
//                        bndl.putString("MSG_COMMAND", "connect");
//                        msg.setData(bndl);
//                        autoHandler.sendMessage(msg);
//                        sleep(100);
//                    }
                    Log.d(TAG, " -> Wait for connect...");
                    while (!isBtConnected){
                        sleep(100);
                    }
                    Log.d(TAG, "Waiting done.");

                    if(!wrong_address || true){
                        Log.d(TAG, "true address");
                        if(connectedDevice.getName().equals("Cabine")){
//                        if(background_send_command){
                            // send command (format: lift_<pos>)
                            Log.d(TAG, " -> Send command " + String.format("lift_%d", position));
                            Message msg = autoHandler.obtainMessage();
                            Bundle bndl = new Bundle();
                            bndl.putString("MSG_COMMAND", String.format("lift_%d", position));
                            msg.setData(bndl);
                            autoHandler.sendMessage(msg);
                            sleep(100);
                        } else {
                            sleep(1000);
                        }
//                        btConnection.SendMessage(String.format("lift_%d", position), true);
//                        sleep(900);
                        // disconnect
                        Log.d(TAG, " -> Disconnect");
                        {
                            Message msg = autoHandler.obtainMessage();
                            Bundle bndl = new Bundle();
                            bndl.putString("MSG_COMMAND", "disconnect");
                            msg.setData(bndl);
                            autoHandler.sendMessage(msg);
                            sleep(100);
                        }
                    } else {
                        Log.d(TAG, "wrong_address");
                    }
                }
                background_wait_for_disconnect = false;

                // Connect to near device
                Log.d(TAG, String.format("Selected device with name: %s (%s)", currentName, currentAddress));
                try {
                    currentFloor = Byte.parseByte(currentName.substring(10));
                } catch (Exception e) {
                    // Maybe string have not int type
                    Log.d(TAG, "Maybe string have not int type");
                    currentFloor = 0;
                }
                Log.d(TAG, String.format("Current floor: %d", currentFloor));
//                if (currentAddress.equals("")) continue;
//                Log.d(TAG, "Test\r\n");
//                {
//                    // save in pref
//                    SharedPreferences.Editor editor = preferences.edit();
//                    editor.putString(BtConsts.MAC_KEY, currentAddress);
//                editor.putString(BtConsts.LAST_NAME, currentName);
//                    editor.apply();
//                    // send command for connect
////                    Log.d(TAG, "Send msg for connect");
////                    Message msg = autoHandler.obtainMessage();
////                    Bundle bndl = new Bundle();
////                    bndl.putString("MSG_COMMAND", "connect");
////                    msg.setData(bndl);
////                    autoHandler.sendMessage(msg);
//
//                }
//                while (!done){
//                    Log.d(TAG, "Wait for done");
//                    sleep(10000);
//                }

                ///////// AFTER NEW //////////////////////////////////////////////////////

            if(!background_auto_version_2) return;
            try{
                Log.d(TAG, "background sleep");
                Thread.sleep(30000);
            } catch (Exception e){
                Log.d(TAG, e.toString());
            }
//            startConnect();

                // Get address and maxfloors from device
//            Log.d(TAG, "Wait for connect...");
//            for (int over = 0; over < OVER; over++){
//                try{
//                    Thread.sleep(100);
//                } catch (Exception e){
//                    //
//                }
//                if(isBtConnected){
//                    Log.d(TAG, "AutoConnected");
//                    break;
//                }
//            }
//            Log.d(TAG, "Reading...");
//            currentAddress = "Reading...";
//            if(currentAddress.equals("Reading...")){
//                return;
//            }
//            if(isBtConnected){
//                Log.d(TAG, "Connected (auto)");
//                Log.d(TAG, "Send msg for address");
//                Message msg = autoHandler.obtainMessage();
//                Bundle bndl = new Bundle();
//                bndl.putString("MSG_COMMAND", "getAddress");
//                msg.setData(bndl);
//                autoHandler.sendMessage(msg);
//            } else {
//                Log.d(TAG, "Connection failed");
//                return;
//            }
//            for(int over = 0; over < OVER; over++){
//                if(!currentAddress.equals("Reading...")){
//                    break;
//                }
//                try{
//                    Thread.sleep(100);
//                } catch (Exception e){}
//            }
//
//            max_floors = -1;
//            if(isBtConnected){
//                Log.d(TAG, "Send msg for max floors");
//                Message msg = autoHandler.obtainMessage();
//                Bundle bndl = new Bundle();
//                bndl.putString("MSG_COMMAND", "getMaxFloors");
//                msg.setData(bndl);
//                autoHandler.sendMessage(msg);
//            }
//            for(int over = 0; over < OVER; over++){
//                if(max_floors != -1){
//                    break;
//                }
//                try{
//                    Thread.sleep(100);
//                } catch (Exception e){}
//            }
//
//            // Send command to device
//            if(auto_move_me && homeAddress.equals(currentAddress) && (currentFloor == 1 || currentFloor == my_floor)){
//                int from = currentFloor;
//                int to = my_floor;
//                if (currentFloor == my_floor){
//                    to = 1;
//                }
//                Log.d(TAG, "Send msg for address");
//                Message msg = autoHandler.obtainMessage();
//                Bundle bndl = new Bundle();
//                bndl.putString("MSG_COMMAND", String.format("liftto_%d_%d", from, to));
//                msg.setData(bndl);
//                autoHandler.sendMessage(msg);
//            }
            }
            Log.d(TAG, "End background_auto!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        };
        Thread autoThread = new Thread(autoRunnable);
        autoThread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void backgroundAutoConnect(){
        if(!btAdapter.isEnabled())return;
        background_auto_connect = true;
        Runnable autoConnectRunnable = () -> {
            while (background_auto_connect){
//                List<Elevator> elevators = listSavedElevators;
//                Log.d(TAG, "for elevators");
                for(Elevator elv : listSavedElevators){
                    String flName = "Elevator_f" + elv.getFloor();
//                    Log.d(TAG, "for floors)");
                    for(Device dvc : elv.getFloors()){
//                        Log.d(TAG, "   - " + dvc.getName());
                        if(flName.equals(dvc.getName())){
//                            btConnection.conn(dvc.getAddress());
                            // conn
                            Log.d(TAG, " -> Conn("+dvc.getAddress()+")");
                            Message msg = autoHandler.obtainMessage();
                            Bundle bndl = new Bundle();
                            bndl.putString("MSG_COMMAND", "conn_"+dvc.getAddress());
                            msg.setData(bndl);
                            autoHandler.sendMessage(msg);
                            sleep(100);
                        }
                    }
                }
                try{
                    Log.d(TAG, "auto connect background sleep");
                    Thread.sleep(20 * 1000);
                } catch (Exception e){
                    Log.d(TAG, e.toString());
                }
                if(isBtConnected && connectedDevice.getName().substring(0, 10).equals("Elevator_f")){
                    // disconnect
                    Log.d(TAG, " -> Disconnect");
                    Message msg = autoHandler.obtainMessage();
                    Bundle bndl = new Bundle();
                    bndl.putString("MSG_COMMAND", "disconnect");
                    msg.setData(bndl);
                    autoHandler.sendMessage(msg);
                    sleep(100);
                    for(Elevator elv : listSavedElevators){
                        for(Device fl : elv.getFloors()){
                            if(fl.getAddress().equals(connectedDevice.getAddress())){
                                if(fl.getName().equals("Elevator_1")){
                                    position = elv.getFloor();
                                } else {
                                    position = 1;
                                }
                                for(byte i = 0; i < 5; i++){
                                    for(Device cb : elv.getCabins()){
                                        // conn
                                        Log.d(TAG, " -> Conn("+cb.getAddress()+")");
                                        msg = autoHandler.obtainMessage();
                                        bndl = new Bundle();
                                        bndl.putString("MSG_COMMAND", "conn_"+cb.getAddress());
                                        msg.setData(bndl);
                                        autoHandler.sendMessage(msg);
                                        sleep(100);
                                    }
                                    try{
                                        Log.d(TAG, "auto connect background sleep");
                                        Thread.sleep(10 * 1000);
                                    } catch (Exception e){
                                        Log.d(TAG, e.toString());
                                    }
                                    if(isBtConnected){
                                        background_wait_for_disconnect = true;
                                        background_send_command = true;
                                        return;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "backgroundAutoConnect() - END");
        };
        Thread autoConnectThread = new Thread(autoConnectRunnable);
        autoConnectThread.start();
    }



    private void getCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_DENIED){
            Log.d(TAG, "Camera permission denied");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        } else {
            isCameraPermissionGranted = true;
        }
    }



    private void getBtPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, BT_REQUEST_PERMISSION);
        }else{
            isBtPermissionGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == BT_REQUEST_PERMISSION) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                isBtPermissionGranted = true;
            } else {
                Toast toast = Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT);
                toast.show();
            }
        } else if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isCameraPermissionGranted = true;
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        // Фильтра для получения только нужного ответа от системы (bluetooth.discover)
        IntentFilter f1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter f2 = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        IntentFilter f3 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, f1);
        registerReceiver(broadcastReceiver, f2);
        registerReceiver(broadcastReceiver, f3);
        ch();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive()");
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, String.format("%s (%s): RSSI=%d", device.getName(), device.getAddress(), rssi));
                boolean hasAddress = false;

                // Ждем пока не освободится массив от других процессов
                while(access_to_list_discovered_devices){
                    sleep(200);
                }
                access_to_list_discovered_devices = true;
                for(DiscoveredDevice dvc : listDiscoveredDevices){
                    if(dvc.Address.equals(device.getAddress())){
                        dvc.rssi.add(rssi);
                        hasAddress = true;
                        break;
                    }
                }
                if(!hasAddress){
                    DiscoveredDevice dvc = new DiscoveredDevice();
                    dvc.Name = device.getName();
                    dvc.Address = device.getAddress();
                    dvc.rssi = new ArrayList<>();
                    dvc.rssi.add(rssi);
                    listDiscoveredDevices.add(dvc);
                }
                access_to_list_discovered_devices = false;

                // check for autoconnect
                String address = "";
                String name = "";
                for(Elevator elv : listSavedElevators){
                    // need fix
                    if(elv.isAuto()){
                        for(Device item : elv.getFloors()){
                            if (device.getAddress().equals(item.getAddress())){
                                Log.d(TAG, "AutoConnect to floor " + item.getAddress());
                                // my floor address, need to connect
                                address = item.getAddress();
                                name = item.getName();
                                background_send_command = false;
                                break;
                            }
                        }
                        for(Device item : elv.getCabins()){
                            if(device.getAddress().equals(item.getAddress())){
                                Log.d(TAG, "AutoConnect to cabine " + item.getAddress());
                                // my cabine address, need to connect
                                address = item.getAddress();
                                name = item.getName();
                                background_send_command = true;
                                break;
                            }
                        }
                    }
                }

                // connect
                // set in preferences dvc.Address
                if(!address.equals("")) {
                    wrong_address = false;
                    Log.d(TAG, " -> set in preferences address: <" + address + ">");
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(BtConsts.MAC_KEY, address);
                    editor.putString(BtConsts.LAST_NAME, name);
                    editor.apply();
                    // connect
                    Log.d(TAG, " -> Connect");
                    if (btConnection.getConnectState() == 0 && !btConnection.isConnecting())
                        btConnection.connect();
                    // wait and disconnect moved on background auto
                    background_wait_for_disconnect = true;
//                position = Byte.parseByte("" + pos);
                } else {
                    wrong_address = true;
                }


                //Toast.makeText(context, "Discovered device with name: " + device.getName(), Toast.LENGTH_SHORT);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                Log.d(TAG, " Discover ended!");
                isBtDiscovering = false;
                countDiscover++;
                Log.d(TAG, String.format("Discover count %d", countDiscover));
                if(call_when_discover_end){
                    call_when_discover_end = false;
                    Call(position);
                }
                if(auto_call_when_discover_end){
                    auto_call_when_discover_end = false;
                    Call_auto(position);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "ACTION_BOND_STATE_CHANGED");
            }
        }
    };

    private void sleep(int ms){
        try{
            Thread.sleep(ms);
        } catch (Exception e){
            Log.d(TAG, "" + e);
         }
    }
}