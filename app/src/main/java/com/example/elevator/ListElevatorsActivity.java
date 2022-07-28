package com.example.elevator;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.example.elevator.adapter.Address_adapter;
import com.example.elevator.adapter.BtConsts;
import com.example.elevator.objects.Device;
import com.example.elevator.objects.Elevator;
import com.example.elevator.objects.Storage;

import java.util.List;

public class ListElevatorsActivity extends AppCompatActivity {
    final String TAG = this.getClass().getSimpleName();
//    String TAG = "Debug";
    private SharedPreferences preferences; // Объявляем переменную (класс) для хранения простых типов данных в памяти
    private ListView listView;
//    private List<ListItemAddress> listAddresses;
    private List<Elevator> listElevators;
    private Address_adapter adapter;
    private boolean listen_response_adapter = true; // Для background response


    private Runnable response_adapter = new Runnable() {
        @Override
        public void run() {
            while (listen_response_adapter){
                if(adapter.isChanged()){
                    Log.d(TAG, "adapter was changed");
//                        adapter.notifyDataSetChanged();
                    Message msg = responseHandler.obtainMessage();
                    Bundle bndl = new Bundle();
                    bndl.putString("MSG_COMMAND", "changed");
                    msg.setData(bndl);
                    responseHandler.sendMessage(msg);
                }
                try{
                    Thread.sleep(100);
                } catch (Exception e){
                    //
                }
            }
        }
    };
    private Thread responseThread = new Thread(response_adapter);

    @SuppressLint("HandlerLeak")
    private Handler responseHandler = new Handler(){
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bndl = msg.getData();
            String command = bndl.getString("MSG_COMMAND");
            if(command == "changed"){
                Log.d(TAG, "changed");
                change();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void change(){
//        SetAddressInPreferences storage = new SetAddressInPreferences(getBaseContext());
        Storage storage = new Storage(getBaseContext());
//        listAddresses = storage.getStorage();
        listElevators = storage.getStorage();
        adapter = new Address_adapter(this, R.layout.item_address, listElevators);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        adapter.setChanged(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Activities", "ListElevatorsActivity().onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_elevators);

        preferences = this.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
        init();

//        adapter.notifyDataSetChanged();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void init(){
        Log.d(TAG, "init()");
        listView  = findViewById(R.id.listView);
        // list заполнить из preferences

//        List<ListItemAddress> list = new ArrayList<>();
//        SetAddressInPreferences storage = new SetAddressInPreferences(this);
        Storage storage = new Storage(this);
        listElevators = storage.getStorage();



        for(Elevator i : listElevators){
            Log.d("for i : list", "Item: " + i.getId());
        }

        Log.d(TAG, "init Adapter...");
        adapter = new Address_adapter(this, R.layout.item_address, listElevators);
        Log.d(TAG, "running Adapter...");
        listView.setAdapter(adapter);

        Log.d(TAG, String.format("list.size() in ListElevatorsActivity = %d", listElevators.size()));
        Log.d(TAG, String.format("listView.getChildCoutn() in ListElevatorsActivity = %d", listView.getChildCount()));


        responseThread.start();

//        listView.setOnItemClickListener((parent, view, position, id) -> {
//            Log.d(TAG, "Something pressed");
//            Log.d(TAG, view.toString() + ", " + position + ", " + id);
//            if(position + 1 == list.size()){
//                Log.d(TAG, "Pressed 'Add' button");
//                Intent settings_activity = new Intent(ListElevatorsActivity.this, Settings.class);
//                startActivity(settings_activity);
//            }
//        });

//        AdapterView.OnItemClickListener listener = listView.getOnItemClickListener();
//        listener.onItemClick(() -> {
//            //
//        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        super.onStart();

        Log.d("Activities", "ListElevatorsActivity().onStart()");
//        SetAddressInPreferences storage = new SetAddressInPreferences(this);
        Storage storage = new Storage(this);
        listElevators = storage.getStorage();
        adapter = new Address_adapter(this, R.layout.item_address, listElevators);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        Log.d("Activities", "ListElevatorsActivity().onResume()");
        super.onResume();
        Storage storage = new Storage(this);
        listElevators = storage.getStorage();
        if(parseQR() == false){
            Toast.makeText(getApplicationContext(),
                    getResources().getString(com.google.zxing.client.android.R.string.msg_wrong_qr),
                    Toast.LENGTH_LONG).show();
        }
    }

    private long StringHEXtoInt(String h){
        Log.d(TAG, "Parse HEX = " + h);
        long res = 0;
        for(char c : h.toCharArray()){
            int bit = 0;
            switch (c){
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '0':
                    bit = Integer.parseInt("" + c);
                    break;
                case 'a':
                    bit = 10;
                    break;
                case 'b':
                    bit = 11;
                    break;
                case 'c':
                    bit = 12;
                    break;
                case 'd':
                    bit = 13;
                    break;
                case 'e':
                    bit = 14;
                    break;
                case 'f':
                    bit = 15;
                    break;
                default:
                    Log.d(TAG, "Parsing fault, num have another sumbols (not HEX)!!!");
                    return -1;
            }
            res *= 16;
            res += bit;
        }
        Log.d(TAG, String.format("Parsed: %d", res));
        return res;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean parseQR(){
        Log.d(TAG, "read QR_CODE");
        String qr = preferences.getString(BtConsts.QR_CODE, "-");

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(BtConsts.QR_CODE, "-");
        editor.apply();

//        assert qr != null;
        if(qr.equals("-")) {
            Log.d(TAG, "QR code is empty");
//            return false;
            return true;
        }
        Log.d(TAG, "QR = " + qr);
        String id = qr.substring(0, 10);
        qr = qr.substring(10);
//        Log.d(TAG, id + ":" + qr);
        Storage str = new Storage(this);
        Elevator elv = new Elevator();
        elv.setId(StringHEXtoInt(id));
        elv.setDescription(getResources().getString(com.google.zxing.client.android.R.string.empty));
        if(elv.getId() < 0) {
            Log.d(TAG, "Wrong Elevator ID");
            return false;
        }
        byte g = 0; // Глубина
        StringBuilder mac = new StringBuilder();
        String name = "Cabine";
        for(char c : qr.toCharArray()){
            switch (c){
                case '/':
                    break;
                case '_':
                    // check if there not numeric
                    for(char i : mac.toString().toCharArray()){
                        switch (i){
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                            case '0':
                                break;
                            default:
                                Log.d(TAG, "Wrong QR code, num floor have another sumbols (not number)!!!");
                                return false;
                        }
                    }
                    name = "Elevator_f" + mac.toString();
                    mac = new StringBuilder();
                    g = 0;
                    break;
                default:
                    g++;
                    mac.append(c);
            }
            if(g == 12){
                g = 0;
                Device dvc = new Device(name, mac.toString());
                elv.addDevice(dvc);
                mac = new StringBuilder();
            }
            if(g > 12){
                Log.d(TAG, "Wrong QR code (MAC length > 12)!!!");
                return false;
            }
        }
        str.printElevator(elv);
        str.addElevator(elv);
//        str.clear();
//        str.printAll();
        str.write();
        return true;
    }
}