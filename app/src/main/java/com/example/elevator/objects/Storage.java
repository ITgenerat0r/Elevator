package com.example.elevator.objects;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.elevator.adapter.BtConsts;
import com.example.elevator.adapter.ListItemAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Storage {
    private String TAG = this.getClass().getSimpleName();
    private SharedPreferences preferences;
    private List<Elevator> storage;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public Storage(Context context) {
        Log.d(TAG, "Constructor()");
        this.preferences = context.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
        this.read();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void read(){
        Log.d(TAG, " read()");
        Set<String> nullSet;
        nullSet = new ArraySet<>();
        storage = new ArrayList<>();
        // read Elevators
        Set<String> storageSetElevators;
        storageSetElevators = preferences.getStringSet(BtConsts.LIST_ELEVATORS, nullSet);
        assert storageSetElevators != null;
        for(String item : storageSetElevators) {
            Log.d(TAG, " -> Elevator item = " + item);
            StringBuilder addr = new StringBuilder();
            StringBuilder id = new StringBuilder();
            StringBuilder floor = new StringBuilder();
            boolean auto = false;
            StringBuilder comm = new StringBuilder();
            byte k = 0;
            for (char i : item.toCharArray()) {
                if (i == '/') {
                    k++;
                    continue;
                }
                if (k == 0) {
                    id.append(i);
                } else if (k == 1) {
                    floor.append(i);
                } else if (k == 2) {
                    if (i == '1') {
                        auto = true;
                    }
                } else if (k == 3) {
                    addr.append(i);
                } else {
                    comm.append(i);
                }
            }
            Log.d(TAG, " -> id = " + id.toString());
            Log.d(TAG, " -> floor = " + floor.toString());
            Log.d(TAG, " -> auto = " + auto);
            Log.d(TAG, " -> addr = " + addr.toString());
            Log.d(TAG, " -> comm = " + comm.toString());

            Elevator el = new Elevator();
            el.setId(Integer.parseInt(id.toString()));
            el.setFloor(Byte.parseByte(floor.toString()));
            el.setAuto(auto);
            el.setDescription(addr.toString());
            storage.add(new Elevator());
            storage.set(storage.size() - 1, el);
        }
        // read Devices
        Set<String> storageSetDevices;
        storageSetDevices = preferences.getStringSet(BtConsts.LIST_DEVICES, nullSet);
        assert storageSetDevices != null;
        for(String item : storageSetDevices){
            Log.d(TAG, " -> Device item = " + item);
            StringBuilder id = new StringBuilder();
            StringBuilder name = new StringBuilder();
            StringBuilder addr = new StringBuilder();
            StringBuilder comm = new StringBuilder();
            byte k = 0;
            for (char i : item.toCharArray()) {
                if (i == '/') {
                    k++;
                    continue;
                }
                if (k == 0) {
                    id.append(i);
                } else if (k == 1) {
                    name.append(i);
                } else if (k == 2) {
                    addr.append(i);
                } else {
                    comm.append(i);
                }
            }
            Log.d(TAG, " -> id = " + id.toString());
            Log.d(TAG, " -> name = " + name.toString());
            Log.d(TAG, " -> addr = " + addr.toString());

            Device dvc = new Device(name.toString(), addr.toString());
            this.getById(Integer.parseInt(id.toString())).addDevice(dvc);

        }
        Log.d(TAG, " read() end");
    }




    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void write(){
        Set<String> stringSetElevators = new ArraySet<>();
        Set<String> stringSetDevices = new ArraySet<>();
        for(Elevator item : storage){
            String auto = "0";
            if(item.isAuto()){
                auto = "1";
            }
            stringSetElevators.add(String.format("%d/%d/%s/%s", item.getId(), item.getFloor(), auto, item.getDescription()));
//            stringSetElevators.add(item.getAddress()+'/'+item.getName()+'/'+item.getFloor()+'/'+auto+'/'+item.getComment());
            for(Device dvc : item.getCabins()){
                stringSetDevices.add(String.format("%s/%s/%s", item.getId(), dvc.getName(), dvc.getAddress()));
            }
            for(Device dvc :item.getFloors()){
                stringSetDevices.add(String.format("%s/%s/%s", item.getId(), dvc.getName(), dvc.getAddress()));
            }
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(BtConsts.LIST_ELEVATORS, stringSetElevators);
        editor.putStringSet(BtConsts.LIST_DEVICES, stringSetDevices);
        editor.apply();
    }

    public void deleteElevatorByID(int id){
        storage.remove(this.getById(id));
    }

    public void addElevator(Elevator el){
        storage.add(el);
    }

    public void renewIDs(){
        for(int i = 0; i < this.getLength(); i++){
            storage.get(i).setId(i);
        }
    }

    public Elevator getById(int id){
        if(storage.get(id).getId() == id){
            return storage.get(id);
        }
        for(Elevator i : storage){
            if(i.getId() == id) return i;
        }
        return new Elevator();
    }

    public Elevator getByIndex(int pos){
        return storage.get(pos);
    }
    public int getLength(){
        return storage.size();
    }
}
