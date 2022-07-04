package com.example.elevator.objects;

import com.example.elevator.adapter.ListItemAddress;

import java.util.List;

public class Elevator {
    private List<ListItemAddress> cabins;
    private List<ListItemAddress> floors;
    private String description;
    private byte floor;
    private boolean auto;

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

    public List<ListItemAddress> getCabins() {
        return cabins;
    }

    public void setCabins(List<ListItemAddress> cabins) {
        this.cabins = cabins;
    }

    public void addCabins(ListItemAddress cabine){
        this.cabins.add(cabine);
    }

    public List<ListItemAddress> getFloors() {
        return floors;
    }

    public void setFloors(List<ListItemAddress> floors) {
        this.floors = floors;
    }

    public void addFloor(ListItemAddress floor){
        this.floors.add(floor);
    }
}
