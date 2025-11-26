package com.example.realestate.models;

import androidx.annotation.Keep;

@Keep
public class ModelProperty {

    String id;
    String uid;
    String purpose;
    String category;
    String subCategory;
    String areaSizeUnit;

    String title;
    String description;
    String email;
    String phoneCode;
    String phoneNumber;
    String country;
    String city;
    String state;

    String address;
    String status;

    // ✅ استبدلنا long بـ Long لتجنب كراش لو القيمة null من Firebase
    Long floors;
    Long bedRooms;
    Long bathRooms;

    Long timestamp;   // ✅ كان long
    Double latitude;
    Double longitude;
    Double areaSize;

    Double price;

    boolean favorite;

    public ModelProperty() {}

    public ModelProperty(String id, String uid, String purpose, String category, String subCategory,
                         String areaSizeUnit, String title, String description, String email, String phoneCode,
                         String phoneNumber, String country, String city, String state, String address,
                         String status, Long floors, Long bedRooms, Long bathRooms, Long timestamp,
                         Double latitude, Double longitude, Double areaSize, Double price, boolean favorite) {
        this.id = id;
        this.uid = uid;
        this.purpose = purpose;
        this.category = category;
        this.subCategory = subCategory;
        this.areaSizeUnit = areaSizeUnit;
        this.title = title;
        this.description = description;
        this.email = email;
        this.phoneCode = phoneCode;
        this.phoneNumber = phoneNumber;
        this.country = country;
        this.city = city;
        this.state = state;
        this.address = address;
        this.status = status;
        this.floors = floors;
        this.bedRooms = bedRooms;
        this.bathRooms = bathRooms;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaSize = areaSize;
        this.price = price;
        this.favorite=favorite;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    // getters/setters كما هي (بدّل توقيعات long إلى Long)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public String getAreaSizeUnit() { return areaSizeUnit; }
    public void setAreaSizeUnit(String areaSizeUnit) { this.areaSizeUnit = areaSizeUnit; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneCode() { return phoneCode; }
    public void setPhoneCode(String phoneCode) { this.phoneCode = phoneCode; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getFloors() { return floors; }
    public void setFloors(Long floors) { this.floors = floors; }
    public Long getBedRooms() { return bedRooms; }
    public void setBedRooms(Long bedRooms) { this.bedRooms = bedRooms; }
    public Long getBathRooms() { return bathRooms; }
    public void setBathRooms(Long bathRooms) { this.bathRooms = bathRooms; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getAreaSize() { return areaSize; }
    public void setAreaSize(Double areaSize) { this.areaSize = areaSize; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
