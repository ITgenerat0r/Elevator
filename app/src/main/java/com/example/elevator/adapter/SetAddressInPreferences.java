package com.example.elevator.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SetAddressInPreferences {
    static final String TAG = "SetAddressInPreferences";
    List<ListItemAddress> storage;
    private SharedPreferences preferences;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public SetAddressInPreferences(Context context) {
        Log.d(TAG, "Constructor()");
        this.preferences = context.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
        read();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void read(){
        Log.d(TAG, " read()");
        Set<String> nullSet;
        nullSet = new ArraySet<>();
        storage = new ArrayList<>();
        Set<String> storageSet;
        storageSet = preferences.getStringSet(BtConsts.LIST_ADRESS, nullSet);
        for(String item : storageSet){
            Log.d(TAG, " -> Item = " + item);
            StringBuilder addr = new StringBuilder();
            StringBuilder nm = new StringBuilder();
            StringBuilder floor = new StringBuilder();
            boolean auto = false;
            StringBuilder comm = new StringBuilder();
            byte k = 0;
            for(char i : item.toCharArray()){
                if(i == '/') {
                    k++;
                    if(k < 5) continue;
                }
                if(k == 0){
                    addr.append(i);
                } else if (k == 1){
                    nm.append(i);
                } else if (k == 2){
                    floor.append(i);
                } else if (k == 3) {
                    if( i == '1'){
                        auto = true;
                    }
                } else {
                    comm.append(i);
                }
            }
            Log.d(TAG, " -> addr = " + addr.toString());
            Log.d(TAG, " -> name = " + nm.toString());
            Log.d(TAG, " -> floor = " + floor.toString());
            Log.d(TAG, " -> auto = " + auto);
            Log.d(TAG, " -> comm = " + comm.toString());

            ListItemAddress ti = new ListItemAddress();
            ti.setAddress(addr.toString());
            ti.setName(nm.toString());
            ti.setFloor(Byte.parseByte(floor.toString()));
            ti.setAuto(auto);
            ti.setComment(comm.toString());
            storage.add(new ListItemAddress());
            storage.set(storage.size() - 1, ti);

            Log.d(TAG, " read() end");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void write(){
        Set<String> stringSet = new ArraySet<>();
        for(ListItemAddress item : storage){
            String auto = "0";
            if(item.isAuto()){
                auto = "1";
            }
            stringSet.add(item.getAddress()+'/'+item.getName()+'/'+item.getFloor()+'/'+auto+'/'+item.getComment());
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(BtConsts.LIST_ADRESS, stringSet);
        editor.apply();
    }

    public void add(String address, String name, byte floor, boolean auto, String comment){
        ListItemAddress item = new ListItemAddress();
        item.setComment(comment);
        item.setFloor(floor);
        item.setAuto(auto);
        item.setAddress(address);
        item.setName(name);
        storage.add(item);
    }

    public void addItem(ListItemAddress item){
        storage.add(item);
    }

    public List<ListItemAddress> getStorage(){
        return storage;
    }

    public void setStorage(List<ListItemAddress> list){
        storage = list;
    }

    public void delete(String address){
        Log.d(TAG, "Delete() start");
        for(ListItemAddress item : storage){
            if(item.getAddress().equals(address)){
                storage.remove(item);
                break;
            }
        }
        Log.d(TAG, "Delete() end");
    }

    public ListItemAddress getItemAddress(String address){
        ListItemAddress res = new ListItemAddress();
        for(ListItemAddress item : storage){
            if(item.getAddress().equals(address)){
                res = item;
            }
        }
        return res;
    }

    public List<String> getListAddresses(){
        List<String> list = new ArrayList<>();
        for(ListItemAddress item : storage){
            list.add(new String(item.getAddress()));
//            list.set(list.size(), item.getAddress());
        }
        Log.d(TAG, "getListAddresses() - list");
        for(String i : list){
            Log.d(TAG, i);
        }
        return list;
    }

    public int getLength(){
        return storage.size();
    }

    public ListItemAddress getByIndex(int pos){
        return storage.get(pos);
    }
}

