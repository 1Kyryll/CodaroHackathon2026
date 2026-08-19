package com.example.hackathoncodaro2026.dto;

import java.time.LocalTime;

public class TimeSlotView {

    private LocalTime start;
    private LocalTime end;
    private int booked;
    private int capacity;
    private boolean available;

    public TimeSlotView() {
    }

    public TimeSlotView(LocalTime start, LocalTime end, int booked, int capacity, boolean available) {
        this.start = start;
        this.end = end;
        this.booked = booked;
        this.capacity = capacity;
        this.available = available;
    }

    public String getLabel() {
        return start + " – " + end;
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

    public LocalTime getEnd() {
        return end;
    }

    public void setEnd(LocalTime end) {
        this.end = end;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
