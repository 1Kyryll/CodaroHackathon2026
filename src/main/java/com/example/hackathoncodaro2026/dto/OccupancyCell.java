package com.example.hackathoncodaro2026.dto;

import java.time.LocalTime;

public class OccupancyCell {

    private LocalTime start;
    private int booked;
    private int capacity;
    private String level;
    private boolean bookable;

    public OccupancyCell() {
    }

    public OccupancyCell(LocalTime start, int booked, int capacity, String level, boolean bookable) {
        this.start = start;
        this.booked = booked;
        this.capacity = capacity;
        this.level = level;
        this.bookable = bookable;
    }

    public int getRemaining() {
        return Math.max(0, capacity - booked);
    }

    public LocalTime getStart() {
        return start;
    }

    public void setStart(LocalTime start) {
        this.start = start;
    }

    public int getBooked() {
        return booked;
    }

    public void setBooked(int booked) {
        this.booked = booked;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean isBookable() {
        return bookable;
    }

    public void setBookable(boolean bookable) {
        this.bookable = bookable;
    }
}
