package com.example.elevator.objects;

import com.example.elevator.adapter.ListItemAddress;

import java.util.List;

public class Elevator {
    private List<Device> cabins;
    private List<Device> floors;
    private String description;
    private byte floor;
    private boolean auto;
    private int id;

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
    }

    public void addFloor(Device floor){
        this.floors.add(floor);
    }
}
