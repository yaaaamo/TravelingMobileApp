package com.example.traveling;

import com.google.firebase.firestore.GeoPoint;

public class Place {
    private String id;
    private String name;
    private String category;
    private GeoPoint location;
    private double price;
    private int effortLevel;
    private boolean goodForCold;
    private boolean goodForHeat;
    private boolean goodForRain;
    private String photoReference;

    public Place() {}

    public Place(String id, String name, String category, GeoPoint location,
                 double price, int effortLevel,
                 boolean goodForCold, boolean goodForHeat, boolean goodForRain) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.location = location;
        this.price = price;
        this.effortLevel = effortLevel;
        this.goodForCold = goodForCold;
        this.goodForHeat = goodForHeat;
        this.goodForRain = goodForRain;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public GeoPoint getLocation() { return location; }
    public double getPrice() { return price; }
    public int getEffortLevel() { return effortLevel; }
    public boolean isGoodForCold() { return goodForCold; }
    public boolean isGoodForHeat() { return goodForHeat; }
    public boolean isGoodForRain() { return goodForRain; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setLocation(GeoPoint location) { this.location = location; }
    public void setPrice(double price) { this.price = price; }
    public void setEffortLevel(int effortLevel) { this.effortLevel = effortLevel; }
    public void setGoodForCold(boolean goodForCold) { this.goodForCold = goodForCold; }
    public void setGoodForHeat(boolean goodForHeat) { this.goodForHeat = goodForHeat; }
    public void setGoodForRain(boolean goodForRain) { this.goodForRain = goodForRain; }

    public String getPhotoReference() { return photoReference; }
    public void setPhotoReference(String photoReference) {
        this.photoReference = photoReference;
    }
}