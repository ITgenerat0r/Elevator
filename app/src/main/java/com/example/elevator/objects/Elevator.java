package com.example.elevator.objects;

import java.util.ArrayList;
import java.util.List;

public class Elevator {
    private long id;
    private List<Device> cabins;
    private List<Device> floors;
    private String description;
    private byte floor;
    private byte maxFloor;
    private boolean auto;

    public Elevator() {
        this.id = 0;
        this.cabins = new ArrayList<>();
        this.floors = new ArrayList<>();
        this.description = "";
        this.floor = 0;
        this.maxFloor = 2;
        this.auto = false;
    }

    public byte getMaxFloor() {
        return maxFloor;
    }

    public void setMaxFloor(byte maxFloor) {
        this.maxFloor = maxFloor;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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
