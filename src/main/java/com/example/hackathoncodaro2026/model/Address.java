package com.example.hackathoncodaro2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Embeddable
public class Address {

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String street;

    @NotBlank
    @Size(max = 16)
    @Column(name = "building_number", nullable = false, length = 16)
    private String buildingNumber;

    @NotBlank
    @Pattern(regexp = "\\d{2}-\\d{3}")
    @Column(name = "postal_code", nullable = false, length = 6)
    private String postalCode;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String city = "Warszawa";

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String district;

    public Address() {
    }

    public Address(String street, String buildingNumber, String postalCode, String district) {
        this.street = street;
        this.buildingNumber = buildingNumber;
        this.postalCode = postalCode;
        this.city = "Warszawa";
        this.district = district;
    }

    public String toDisplayString() {
        return street + " " + buildingNumber + ", " + postalCode + " " + city + ", " + district;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }
}
