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

import java.util.List;
import java.util.Set;

public class Storage {
    private String TAG = this.getClass().getSimpleName();
    private SharedPreferences preferences;
    private List<Elevator> storage;

    public Storage(Context context) {
        Log.d(TAG, "Constructor()");
        this.preferences = context.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);

    }

    public void read(){}

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

    public Elevator getByIndex(int pos){
        return storage.get(pos);
    }
    public int getLength(){
        return storage.size();
    }
}
