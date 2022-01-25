package com.example.elevator;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.example.elevator.adapter.Address_adapter;
import com.example.elevator.adapter.BtConsts;
import com.example.elevator.adapter.ListItemAddress;
import com.example.elevator.adapter.SetAddressInPreferences;

import java.util.ArrayList;
import java.util.List;

public class ListElevatorsActivity extends AppCompatActivity {
    final static String TAG = "ListElevatorsActivity";
    private SharedPreferences preferences; // Объявляем переменную (класс) для хранения простых типов данных в памяти
    private ListView listView;
    private List<ListItemAddress> listAddresses;
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
        SetAddressInPreferences storage = new SetAddressInPreferences(getBaseContext());
        listAddresses = storage.getStorage();
        adapter = new Address_adapter(this, R.layout.item_address, listAddresses);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        adapter.setChanged(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        SetAddressInPreferences storage = new SetAddressInPreferences(this);
        listAddresses = storage.getStorage();


        for(ListItemAddress i : listAddresses){
            Log.d("for i : list", "Item: " + i.getAddress());
        }


        adapter = new Address_adapter(this, R.layout.item_address, listAddresses);
        listView.setAdapter(adapter);

        Log.d(TAG, String.format("list.size() in ListElevatorsActivity = %d", listAddresses.size()));
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
        Log.d(TAG, " -> onStart()");
        SetAddressInPreferences storage = new SetAddressInPreferences(this);
        listAddresses = storage.getStorage();
        adapter = new Address_adapter(this, R.layout.item_address, listAddresses);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}