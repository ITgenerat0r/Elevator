package com.example.elevator.objects;

import com.example.elevator.adapter.ListItemAddress;

import java.util.ArrayList;
import java.util.List;

public class Elevator {
    private List<Device> cabins;
    private List<Device> floors;
    private String description;
    private byte floor;
    private byte maxFloor;
    private boolean auto;
    private int id;

    public Elevator() {
        this.cabins = new ArrayList<>();
        this.floors = new ArrayList<>();
        this.description = "";
        this.floor = 0;
        this.maxFloor = 2;
        this.auto = false;
        this.id = 0;
    }

    public byte getMaxFloor() {
        return maxFloor;
    }

    public void setMaxFloor(byte maxFloor) {
        this.maxFloor = maxFloor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte getFloor() {
        return floor;
    }

    public void setFloor(byte floor) {
        this.floor = floor;
    }

    public boolean isAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public List<Device> getCabins() {
        return cabins;
    }

    public void setCabins(List<Device> cabins) {
        this.cabins = cabins;
    }

    public void addCabins(Device cabine){
        this.cabins.add(cabine);
    }

    public List<Device> getFloors() {
        return floors;
    }

    public void setFloors(List<Device> floors) {
        this.floors = floors;
        this.maxFloor = Byte.parseByte(Integer.toString(floors.size()));
    }

    public void addFloor(Device floor){
        this.floors.add(floor);
    }

    public void addDevice(Device d){
        if(d.getName().equals("Cabine")){
            this.addCabins(d);
        } else {
            this.addFloor(d);
        }
    }
}
