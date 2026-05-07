package com.lcyhz.urbanova.entity;

public class GPSData {
    private String deviceId;
    private double latitude;
    private double longitude;
    private int batteryLevel;

    public GPSData() {}

    public GPSData(String deviceId, double latitude, double longitude, int batteryLevel) {
        this.deviceId = deviceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.batteryLevel = batteryLevel;
    }

    // Getter 和 Setter（必须，Spring Boot用它们来获取字段值）
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

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

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    @Override
    public String toString() {
        return "GPSData{" +
                "deviceId='" + deviceId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", batteryLevel=" + batteryLevel +
                '}';
    }
}