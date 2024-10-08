package com.bonipitre.ouestlabonipitre;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MarkerData {
    private double latitude;
    private double longitude;
    private double speed;
    private double altitude;
    private double pressure;
    private String date;
    private String time;
    private long timestamp;  // Add the timestamp field

    // Default constructor required for calls to DataSnapshot.getValue(MarkerData.class)
    public MarkerData() { }

    // Constructor with all parameters (optional)
    public MarkerData(double latitude, double longitude, double speed, double altitude, double pressure, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.altitude = altitude;
        this.pressure = pressure;
        this.timestamp = timestamp;  // Initialize the timestamp
    }

    // Getters and Setters for all fields
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public String getDate() {
        Date date = new Date(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        return formatter.format(date);
    }

    public String getTime() {
        Date date = new Date(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm");
        return formatter.format(date);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
