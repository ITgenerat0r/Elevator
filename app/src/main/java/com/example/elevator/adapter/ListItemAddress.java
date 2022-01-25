package com.example.elevator.adapter;

public class ListItemAddress {
    private String Address;
    private String Comment;
    private byte floor;
    private boolean auto;

    public ListItemAddress() {
        Address = "08:64:00:00:ff";
        Comment = "Description";
        this.floor = 2;
        this.auto = false;
    }

    public boolean isAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public byte getFloor() {
        return floor;
    }

    public void setFloor(byte floor) {
        this.floor = floor;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public String getComment() {
        return Comment;
    }

    public void setComment(String comment) {
        Comment = comment;
    }
}
