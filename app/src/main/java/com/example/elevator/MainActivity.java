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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.elevator.adapter.BtAdapter;
import com.example.elevator.adapter.BtConsts;
import com.example.elevator.adapter.BtnItem;
import com.example.elevator.adapter.ButtonAdapter;
import com.example.elevator.adapter.LiftAdapter;
import com.example.elevator.adapter.ListItem;
import com.example.elevator.adapter.ListItemAddress;
import com.example.elevator.adapter.SetAddressInPreferences;
import com.example.elevator.bluetooth.BtConnection;
import com.example.elevator.bluetoothle.BLEConnection;

import java.lang.reflect.GenericArrayType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MenuItem menuItem;
    private MenuItem menu_list_devices;
    private MenuItem menu_bt_connect;
    private MenuItem menu_test;
    private MenuItem menu_settings;
    private BluetoothAdapter btAdapter;
    private final int ENABLE_REQUEST = 15;
    private final static String TAG = "MainActivity";
    private Button btn_send; // Button for send message to HMSoft
    private EditText input_text; // Field for input commands
    private TextView current_address;
    private TextView current_floor;
    private TextView dialog_history; // поле для вывода информации(Отправленной и полученной, а так же debugs)
    private CheckBox end_of_send;
    private boolean bluetooth_status; // Power_button bluetooth
    private boolean BLE_state; // status Bluetooth connecting button
    private SharedPreferences preferences; // value for save information in storage
    private BLEConnection btConnection;

    private Handler handler;
    private List<String> log;

    private GridView gridView;
    private List<BtnItem> list_btn; // Список объектов кнопок лифта
    private ButtonAdapter btnAdapter; // Адаптер для кнопок лифта
    private LiftAdapter liftAdapter;
    private byte max_floors = 2; // количество этажей
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
    private boolean background_auto_discover = true;
    private byte position = 0; // Номер нажатой кнопки, хранится здесь для команды вызова и др.
    private boolean call_when_discover_end = false;
    private boolean done = false; // for connect in background
    private boolean isBtConnected = false;
    private final int OVER = 2000;
    private final int BT_REQUEST_PERMISSION = 111; // Code for broadcasteceiver
    private final byte COUNT_DISCOVERS = 1;  // Количество попыток обнаружить устройства
    private final byte UPDATE_LIST_AFTER = 3;
    private byte count_for_update_list = 0;
    private boolean isBtPermissionGranted = false; // Разрешение геолокации
    private boolean isBtDiscovering = false; // State discovering
    private byte countDiscover = 0; // Количество обнаружений
    private boolean background_wait_for_disconnect = false;
    private boolean background_send_command = false;
    private List<DiscoveredDevice> listDiscoveredDevices = new ArrayList<>(); // Хранит информацию об обнаруженных устройствах
    private boolean access_to_list_discovered_devices = false; // true если идет цикл по listDiscoveredDevices,
                                                                // нужен что бы не произошло одновременного обращения к списку
    private boolean wrong_address = false; // если при автоматическом режиме не нашлось адресов сохраненных в памяти, то будет равно true
    //
    private List<ListItemAddress> listSavedAddresses;


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
//            Log.d("MainLog", "bgHandler()");
            super.handleMessage(msg);
            Bundle bndl = msg.getData();
            ArrayList<String> data_response = bndl.getStringArrayList("MSG_LIST_KEY");
            // Выполняем нужное действие если bgHandler отправил sendMessage()
//            Log.d("MainLog", "Size data_response: " + data_response.size());
            for(String i : data_response){
//                Log.d("MainLog", "Getting data: " + i);
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
            if(command.equals("discover")){
                // discover devices
                if(isBtPermissionGranted && btAdapter.isEnabled()){
                    if(!isBtDiscovering){
                        Log.d("MainLog", "Start discovering...");
                        isBtDiscovering = true;
                        btAdapter.startDiscovery();
                    }
                }
            } else if (command.equals("connect")){
                if(!BLE_state){
//                    btConnection.connect();
//                    BLE_state = true;
                    startConnect();
//                    menu_bt_connect.setIcon(R.drawable.ic_connected);
                }
            } else if (command.equals("disconnect")){
                btConnection.disconnect();
                current_address.setText("Лифт вызван.");
            } else {
                if(isBtConnected){
                    btConnection.SendMessage(command, true);
                }
            }
            Log.d("MainLog", "end autoHandler (msg)");
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
        Log.d("MainLog", "Wait for connect...");
        for (int over = 0; over < 600; over++){
            sleep(100);
            if(btCon.getConnectState() == 2){
                Log.d("MainLog", "AutoConnected");
                break;
            }
        }
        Log.d("MainLog", "Reading...");
        currentAddress = "Reading...";
//        if(true) return;
        sleep(2500);
        if(btCon.getConnectState() == 2){
            Log.d("MainLog", "Connected (auto)");
            Log.d("MainLog", "Send msg for address");
            btCon.SendMessage("getAddress", true);
        } else {
            Log.d("MainLog", "Connection failed");
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
            Log.d("MainLog", "Send msg for max floors");
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
            Log.d("MainLog", "Send command");
            btCon.SendMessage(String.format("liftto_%d_%d", from, to), true);
        }
        sleep(2500);
        btCon.disconnect();
        sleep(2500);
        for(int over = 0; over < OVER; over++){
            if(btCon.getConnectState() == 0)break;
        }
        btCon = null;
        Log.d("MainLog", "End startConnect()");
//        backgroundResponse();
        done = true;
    };

    @SuppressLint("HandlerLeak")
    private Handler responseHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            isBtConnected = (btConnection.getConnectState() == 2);
            current_address.setText(String.format("isBtConnected = %b", isBtConnected));
            readResponse(btConnection);
            if(currentFloor > 0){
                current_floor.setText(currentFloor);
            }
        }
    };

    private void readResponse(BLEConnection con){
//        Log.d("MainLog", "readResponse()");
        if(con != null && con.isHaveData() > 0){
//            Log.d("MainLog", "con.getResponse()");
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
                            Log.e("MainLog", "g is have wrong type (not byte)");
                        }
                    } else if (i.substring(0,4).equals("addr")){
                        if(i.length() > 4){
                            String address = i.substring(4);
                            Log.d("MainLog", "Elevator ID: " + address);
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
                        Log.d("MainLog", "Speed <- RX");
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
        Log.d("MainLog", "\n\r.\n\r.\n\r.\n\r.\n\r.\n\r.\n\r");
        Log.d("MainLog", "Start program...");
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        init();
        getBtPermission();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void init(){
        BLE_state = false;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btn_send = findViewById(R.id.bt_send);
        btn_send.setOnClickListener(this::onClick);
        input_text = findViewById(R.id.input_text);
        current_address = findViewById(R.id.address);
        current_floor = findViewById(R.id.floor);
        dialog_history = findViewById(R.id.dialog_history);
        end_of_send = findViewById(R.id.checkBox_at);
        preferences = getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
        Log.d("MainLog", "MAC in preferences: " + preferences.getString(BtConsts.MAC_KEY, "none"));
        homeAddress = preferences.getString(BtConsts.MY_ADDRESS, "none");
        my_floor = preferences.getInt(BtConsts.MY_FLOOR, 0);
        auto_move_me = preferences.getBoolean(BtConsts.LIFT_ME, false);
        Toast toast = Toast.makeText( this , "MAC in preferences: " + preferences.getString(BtConsts.MAC_KEY, "none"), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 10 , 20);
        toast.show();
        btConnection = new BLEConnection(this);


        list_btn = new ArrayList<BtnItem>();
        gridView = findViewById(R.id.list_buttons);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
//            Toast.makeText(getApplicationContext(), "Clicked " + (position + 1), Toast.LENGTH_SHORT).show();
            Call(position + 1);
            // Test for speed
//            if(btConnection != null && btConnection.getConnectState() == 2){
//                DateFormat tm = new SimpleDateFormat("KK:mm:ss", Locale.getDefault());
//                String time = tm.format(new Date());
//                Log.d("MainLog", "Speed -> TX");
//                dialog_history.append("Speed -> TX   [" + time + "]\r\n");
//                start = System.currentTimeMillis();
//                btConnection.SendMessage("speed", true);
////                btConnection.SendMessage("call_" + (position + 1));
//            }
        });


        listFloors = new ArrayList<String>();
        for(int i = 0; i < max_floors; i++){
            listFloors.add("0");
        }


        liftAdapter = new LiftAdapter(MainActivity.this, listFloors);
        gridView.setAdapter(liftAdapter);



//        liftAdapter.notifyAll();
//        btnAdapter = new ButtonAdapter(this, R.layout.button_item, list_btn);
//        gridView.setAdapter(btnAdapter);


//        BtnItem bt = new BtnItem(1);
//        list_btn.add(bt);
//        bt = new BtnItem(2);
//        list_btn.add(bt);
//        btnAdapter.notifyDataSetChanged();

//        View tmp = LayoutInflater.from(getParent().getApplicationContext()).inflate(R.layout.button_item, null, false);


//        handler = new Handler();
//        handler.post(() -> {
//            BLEConnection bt;
//            bt = new BLEConnection(this);
//            while (true) {
//
//            }
//        });
        backgroundResponse();
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void Call(int pos){
        Log.d("MainLog", "      ->  Call();");
        position = Byte.parseByte("" + pos);
        current_address.setText(String.format("Идет вызов на %d этаж...", pos));
        Log.d("MainLog", String.format("Идет вызов на %d этаж...", pos));

        if(isBtDiscovering){
            call_when_discover_end = true;
            return;
        }
        Log.d("MainLog", "      ->  step");
        DiscoveredDevice call_dvc = new DiscoveredDevice();
        Log.d("MainLog", "      ->  step1");
        call_dvc.Name = "Empty";
        call_dvc.Address = "None";
        Log.d("MainLog", "      ->  step2");
        call_dvc.rssi = new ArrayList<Integer>();
        Log.d("MainLog", "      ->  step3");
        call_dvc.rssi.add(1000);
        Log.d("MainLog", "      ->  step4");
        boolean isCabine = false;
        Log.d("MainLog", "      ->  step5");
        String nm = "Elevator_f";
        if(pos < 10){
            nm += "0";
        }
        nm += pos;

        if(!access_to_list_discovered_devices) {
            access_to_list_discovered_devices = true;
            for (DiscoveredDevice dvc : listDiscoveredDevices) {
                Log.d("MainLog", "      ->  ->  step - " + dvc.Name);
                if (dvc.Name.equals(nm)) {
                    Log.d("MainLog", "      ->  ->  step1true");
                    if (call_dvc.Name.equals("Cabine")) {
                        Log.d("MainLog", "      ->  ->  step2true");
                        if (dvc.rssi.get(0) < call_dvc.rssi.get(0)) {
                            Log.d("MainLog", "      ->  ->  step3true");
                            call_dvc = dvc;
                            isCabine = false;
                            break;
                        }
                    } else {
                        Log.d("MainLog", "      ->  ->  step2false");
                        call_dvc = dvc;
                        isCabine = false;
                    }
                } else if (dvc.Name.equals("Cabine")) {
                    Log.d("MainLog", "      ->  ->  step1false");
                    if (call_dvc.Name.equals("Empty")) {
                        Log.d("MainLog", "      ->  ->  step2true");
                        call_dvc = dvc;
                        isCabine = true;
                    } else {
                        Log.d("MainLog", "      ->  ->  step2false");
                        if (call_dvc.rssi.get(0) >= dvc.rssi.get(0)) {
                            Log.d("MainLog", "      ->  ->  step3true");
                            call_dvc = dvc;
                            isCabine = true;
                        }
                    }
                }
            }
            access_to_list_discovered_devices = false;
        } else {
            String.format("No access, BT is busy");
            call_when_discover_end = true;
            return;
        }

        Log.d("MainLog", "      ->  step6");
            if (!call_dvc.Name.equals("Empty")) {
                // set in preferences dvc.Address
                Log.d("MainLog", " -> set in preferences dvc.Address " + call_dvc.Address + " from Name: " + call_dvc.Name);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(BtConsts.MAC_KEY, call_dvc.Address);
                editor.apply();
                // connect
                Log.d("MainLog", " -> Connect");
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

        Log.d("MainLog", "      ->  step7");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menuItem = menu.findItem(R.id.id_bt_button);
        menu_list_devices = menu.findItem(R.id.id_menu);
        menu_bt_connect = menu.findItem(R.id.id_bt_connect);
        menu_test = menu.findItem(R.id.id_bt_test);
        menu_settings = menu.findItem(R.id.id_settings);

        setBtIcon();

        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        homeAddress = preferences.getString(BtConsts.MY_ADDRESS, "none");
        my_floor = preferences.getInt(BtConsts.MY_FLOOR, 0);
        auto_move_me = preferences.getBoolean(BtConsts.LIFT_ME, false);


        Log.d(TAG, "OnStart()");

        SetAddressInPreferences storage = new SetAddressInPreferences(this);
        listSavedAddresses = storage.getStorage();
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
                menuItem.setTitle("Enable Bluetooth");
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
            if(!BLE_state){
                btConnection.connect();
                BLE_state = true;
                menu_bt_connect.setIcon(R.drawable.ic_connected);
            } else {
                btConnection.disconnect();
                BLE_state = false;
                menu_bt_connect.setIcon(R.drawable.ic_connection);
            }
        } else if (item.getItemId() == R.id.id_settings) {
            setBtIcon();
            if(!bluetooth_status){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth выключен");
                builder.setMessage("Вклчить bluetooth?");
                builder.setPositiveButton("Включить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((MainActivity) getActivity()).enableBt();
                    }
                });
                builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do something if pressed button "Cancel"
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
            if(bluetooth_status){
                Intent settings_activity = new Intent(MainActivity.this, ListElevatorsActivity.class);
                startActivity(settings_activity);
            }

        } else if (item.getItemId() == R.id.id_bt_test){
            if(!test_state){
                menu_test.setIcon(R.drawable.ic_test_on);
                test_state = true;
                if(!BLE_state) {
//                    menu_bt_connect.setEnabled(false);
                    backgroundAuto();
                }
            } else {
                menu_test.setIcon(R.drawable.ic_test_off);
                test_state = false;
                background_auto_discover = false;
                background_auto = false;

            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("MainLog", "onActivityResult()");
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
            menuItem.setTitle("Disable Bluetooth");
            menu_list_devices.setEnabled(true);
            menu_bt_connect.setEnabled(true);
            bluetooth_status = true;
        } else {
            menuItem.setIcon(R.drawable.ic_bt_disabled);
            menuItem.setTitle("Enable Bluetooth");
            menu_list_devices.setEnabled(false);
            menu_bt_connect.setEnabled(false);
            bluetooth_status = false;
        }
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
                }
                if(btConnection != null && btConnection.getConnectState() == 2){
                    btConnection.SendMessage(text, !end_of_send.isChecked());
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
            default:
                break;
        }
    }

    private Object getActivity() {
        return this;
    }

    private void backgroundResponse(){
        ////////// BACKGROUND /////////////////////
        Log.d("MainLog", "Start backgroundResponse");
        background_loop = true;
        Runnable bgRunnable = () -> {
            List<String> response_data = new ArrayList<>();
//            response_data.add("Test background");
            while(background_loop){
//                        Log.d("MainLog", "Loop in background");
                if(response_data.size() > 0){
                    Log.d("MainLog", "We have data!");
                    ArrayList<String> msg_data = new ArrayList<>();
                    for(String i : response_data){
                        msg_data.add(i);
                        Log.d("MainLog", "Added data: " + i);
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

        //////////// BACKGROUND END /////////////////
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void backgroundAuto(){
        if(!btAdapter.isEnabled())return;
        background_auto = true;
        background_auto_discover = true;
        @SuppressLint("DefaultLocale") Runnable autoRunnable = () -> {
            // Discover devices
            while (background_auto) {
                Log.d("MainLog", "Next in backgroundAuto");
                countDiscover = 0;
                // Очистка массива listDiscoveredDevices для обновления данных
                if(count_for_update_list >= UPDATE_LIST_AFTER){
                    listDiscoveredDevices.clear();
                }
                while ((countDiscover < COUNT_DISCOVERS) && background_auto_discover) {
                    if (!isBtDiscovering) {
                        Log.d("MainLog", "  Send msg for discovering");
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
                        Log.d("MainLog", "  not sleep");
                    }
                }
                count_for_update_list++;
                Log.d("MainLog", "  All discovers vere done!");
                // Print list of discovered devices
                String currentAddress = "";
                String currentName = "";
                // Определение близжайшего устройсва по данным сканирвания
                int currentRSSI = 1000;
                Log.d("MainLog", "Loop in discovered devices");

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
                    Log.d("MainLog", "Name " + dvc.Name + " (" + dvc.Address + "):" + RSSIs);
                    Log.d("MainLog", String.format("count: %d", count));
                    if (tmp < currentRSSI && count == COUNT_DISCOVERS && dvc.Name != null && dvc.Name.length() > 8 && dvc.Name.substring(0, 8).equals("Elevator")) {
                        Log.d("MainLog", "Set this device\r\n");
                        currentAddress = dvc.Address;
                        currentName = dvc.Name;
                    }
                }
                access_to_list_discovered_devices = false;

                Log.d("MainLog", "End loop");

                if(background_wait_for_disconnect){
//                    {
//                        Log.d("MainLog", " -> Connecting...");
//                        Message msg = autoHandler.obtainMessage();
//                        Bundle bndl = new Bundle();
//                        bndl.putString("MSG_COMMAND", "connect");
//                        msg.setData(bndl);
//                        autoHandler.sendMessage(msg);
//                        sleep(100);
//                    }
                    Log.d("MainLog", " -> Wait for connect...");
                    while (!isBtConnected){
                        sleep(100);
                    }
                    Log.d("MainLog", "Waiting done.");

                    if(!wrong_address || true){
                        Log.d("MainLog", "true address");
                        if(background_send_command){
                            // send command (format: lift_<pos>)
                            Log.d("MainLog", " -> Send command");
                            Message msg = autoHandler.obtainMessage();
                            Bundle bndl = new Bundle();
                            bndl.putString("MSG_COMMAND", String.format("lift_%d", position));
                            msg.setData(bndl);
                            autoHandler.sendMessage(msg);
                            sleep(100);
                        } else {
                            sleep(1000);
                        }
                        // disconnect
                        Log.d("MainLog", " -> Disconnect");
                        {
                            Message msg = autoHandler.obtainMessage();
                            Bundle bndl = new Bundle();
                            bndl.putString("MSG_COMMAND", "disconnect");
                            msg.setData(bndl);
                            autoHandler.sendMessage(msg);
                            sleep(100);
                        }
                    } else {
                        Log.d("MainLog", "wrong_address");
                    }
                }
                background_wait_for_disconnect = false;

                // Connect to near device
                Log.d("MainLog", String.format("Selected device with name: %s (%s)", currentName, currentAddress));
                try {
                    currentFloor = Byte.parseByte(currentName.substring(10));
                } catch (Exception e) {
                    // Maybe string have not int type
                    currentFloor = 0;
                }
                Log.d("MainLog", String.format("Current floor: %d", currentFloor));
//                if (currentAddress.equals("")) continue;
//                Log.d("MainLog", "Test\r\n");
//                {
//                    // save in pref
//                    SharedPreferences.Editor editor = preferences.edit();
//                    editor.putString(BtConsts.MAC_KEY, currentAddress);
//                    editor.apply();
//                    // send command for connect
////                    Log.d("MainLog", "Send msg for connect");
////                    Message msg = autoHandler.obtainMessage();
////                    Bundle bndl = new Bundle();
////                    bndl.putString("MSG_COMMAND", "connect");
////                    msg.setData(bndl);
////                    autoHandler.sendMessage(msg);
//
//                }
//                while (!done){
//                    Log.d("MainLog", "Wait for done");
//                    sleep(10000);
//                }

                ///////// AFTER NEW //////////////////////////////////////////////////////


            try{
                Thread.sleep(30000);
            } catch (Exception e){
                //
            }
//            startConnect();

                // Get address and maxfloors from device
//            Log.d("MainLog", "Wait for connect...");
//            for (int over = 0; over < OVER; over++){
//                try{
//                    Thread.sleep(100);
//                } catch (Exception e){
//                    //
//                }
//                if(isBtConnected){
//                    Log.d("MainLog", "AutoConnected");
//                    break;
//                }
//            }
//            Log.d("MainLog", "Reading...");
//            currentAddress = "Reading...";
//            if(currentAddress.equals("Reading...")){
//                return;
//            }
//            if(isBtConnected){
//                Log.d("MainLog", "Connected (auto)");
//                Log.d("MainLog", "Send msg for address");
//                Message msg = autoHandler.obtainMessage();
//                Bundle bndl = new Bundle();
//                bndl.putString("MSG_COMMAND", "getAddress");
//                msg.setData(bndl);
//                autoHandler.sendMessage(msg);
//            } else {
//                Log.d("MainLog", "Connection failed");
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
//                Log.d("MainLog", "Send msg for max floors");
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
//                Log.d("MainLog", "Send msg for address");
//                Message msg = autoHandler.obtainMessage();
//                Bundle bndl = new Bundle();
//                bndl.putString("MSG_COMMAND", String.format("liftto_%d_%d", from, to));
//                msg.setData(bndl);
//                autoHandler.sendMessage(msg);
//            }
            }
            Log.d("MainLog", "End background_auto!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        };
        Thread autoThread = new Thread(autoRunnable);
        autoThread.start();
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
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    protected void onResume() {
        Log.d("MainLog", "onResume()");
        super.onResume();
        // Фильтра для получения только нужного ответа от системы (bluetooth.discover)
        IntentFilter f1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter f2 = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        IntentFilter f3 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, f1);
        registerReceiver(broadcastReceiver, f2);
        registerReceiver(broadcastReceiver, f3);
    }

    @Override
    protected void onPause() {
        Log.d("MainLog", "onPause()");
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainLog", "onReceive()");
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("MainLog", String.format("%s (%s): RSSI=%d", device.getName(), device.getAddress(), rssi));
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
                for(ListItemAddress item : listSavedAddresses){
                    if(item.isAuto()){
//                        String add = ":";
//                        if (item.getFloor() < 10) add = ":0";
                        if(device.getAddress().equals(item.getAddress())){
                            Log.d(TAG, "AutoConnect to " + item.getAddress());
                            // my cabine address, need to connect
                            address = item.getAddress();
                            background_send_command = true;
                            break;
                        } else if (device.getAddress().equals(item.getAddress())){
                            Log.d(TAG, "AutoConnect to " + item.getAddress());
                            // my floor address, need to connect
                            address = item.getAddress();
                            background_send_command = false;
                            break;
                        }
                    }
                }

                // connect
                // set in preferences dvc.Address
                if(address != "") {
                    wrong_address = false;
                    Log.d("MainLog", " -> set in preferences address: <" + address + ">");
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(BtConsts.MAC_KEY, address);
                    editor.apply();
                    // connect
                    Log.d("MainLog", " -> Connect");
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
                Log.d("MainLog", " Discover ended!");
                isBtDiscovering = false;
                countDiscover++;
                Log.d("MainLog", String.format("Discover count %d", countDiscover));
                if(call_when_discover_end){
                    call_when_discover_end = false;
                    Call(position);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("MainLog", "ACTION_BOND_STATE_CHANGED");
            }
        }
    };

    private void sleep(int ms){
        try{
            Thread.sleep(ms);
        } catch (Exception e){
            Log.d("MainLog", "" + e);
        }
    }
}