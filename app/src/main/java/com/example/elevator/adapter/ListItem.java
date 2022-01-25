package com.example.elevator.adapter;

import android.bluetooth.BluetoothDevice;

public class ListItem { // Структура, для передачи сразу двух строк в class BtAdapter

    private BluetoothDevice btDevice;
    private String itemType = BtAdapter.DEF_ITEM_TYPE;
    private String rssi = "";

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public BluetoothDevice getBtDevice() {
        return btDevice;
    }

    public void setBtDevice(BluetoothDevice btDevice) {
        this.btDevice = btDevice;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
}
