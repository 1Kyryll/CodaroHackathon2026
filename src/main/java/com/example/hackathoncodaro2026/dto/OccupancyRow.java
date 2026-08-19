package com.example.hackathoncodaro2026.dto;

import java.util.ArrayList;
import java.util.List;

public class OccupancyRow {

    private Long resourceId;
    private String resourceName;
    private String facilityName;
    private String sport;
    private String imagePath;
    private int capacity;
    private List<OccupancyCell> cells = new ArrayList<>();

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<OccupancyCell> getCells() {
        return cells;
    }

    public void setCells(List<OccupancyCell> cells) {
        this.cells = cells;
    }
}
