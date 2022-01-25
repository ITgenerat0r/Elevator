package com.example.elevator;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.elevator.adapter.BtAdapter;
import com.example.elevator.adapter.ListItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BtListActivity extends AppCompatActivity {
    private final int BT_REQUEST_PERMISSION = 111;
    private ListView listView;
    private List<ListItem> list;
    private BtAdapter adapter;
    private BluetoothAdapter btAdapter;
    private boolean isBtDiscovering = false;
    private boolean isBtPermissionGranted = false;
    private boolean isDiscovery = false;
    private ActionBar ab;



    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_list);
        //getBtPermission();
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bt_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            // button "back"
            if(isBtDiscovering){
                btAdapter.cancelDiscovery();
                isBtDiscovering = false;
                ab.setTitle(R.string.app_name);
                getPairedDevices();
            } else {
                finish();
            }

        } else if (item.getItemId() == R.id.id_discovery){ // id_search in video
             // button "discovery"
            getBtPermission();
            if(isBtPermissionGranted){
                if(!isBtDiscovering){
                    list.clear();
                    ab.setTitle(R.string.discovering);
                    ListItem itemTitle = new ListItem();
                    itemTitle.setItemType(BtAdapter.TITLE_ITEM_TYPE);
                    list.add(itemTitle);
                    adapter.notifyDataSetChanged();
                    isBtDiscovering = true;
                    btAdapter.startDiscovery();
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void init(){
        ab = getSupportActionBar();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        list = new ArrayList<>();
        ActionBar ab = getSupportActionBar();
        if(ab == null)return;
        ab.setDisplayHomeAsUpEnabled(true);

        listView = findViewById(R.id.listView);
        adapter = new BtAdapter(this, R.layout.bt_list_item, list);
        listView.setAdapter(adapter);
        getPairedDevices();
        onItemClickListener();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void onItemClickListener(){
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ListItem item = (ListItem) parent.getItemAtPosition(position);
            if(item.getItemType().equals(BtAdapter.DISCOVERY_ITEM_TYPE)) item.getBtDevice().createBond();
        });
    }


    private void getPairedDevices(){
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if(pairedDevices.size() > 0){
            // There are paired devices. Get the name and address of each paired device.
            list.clear();
            for (BluetoothDevice device: pairedDevices){
                ListItem item = new ListItem();
                item.setBtDevice(device);
                item.setRssi("");
                list.add(item);
            }
            adapter.notifyDataSetChanged();
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

    private void getBtPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, BT_REQUEST_PERMISSION);
        }else{
            isBtPermissionGranted = true;
        }
    }

    @Override
    protected void onResume() {
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
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("MainLog", String.format("%s: RSSI=%d",device.getName(), rssi));
                ListItem item = new ListItem();
                item.setBtDevice(device);
                item.setItemType(BtAdapter.DISCOVERY_ITEM_TYPE);
                item.setRssi(String.format("%d", rssi));
                list.add(item);
                adapter.notifyDataSetChanged();

                //Toast.makeText(context, "Discovered device with name: " + device.getName(), Toast.LENGTH_SHORT);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                isBtDiscovering = false;
                getPairedDevices();
                ab.setTitle(R.string.app_name);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() == BluetoothDevice.BOND_BONDED){
                    getPairedDevices();
                }
            }
        }
    };
}