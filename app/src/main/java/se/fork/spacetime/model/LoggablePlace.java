package se.fork.spacetime.model;

import android.util.Log;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import se.fork.spacetime.utils.FlatEarthDist;

/**
 * Created by per.fork on 2017-12-27.
 */

public class LoggablePlace {
    private String id;
    private double latitude;
    private double longitude;
    private String name;
    private String address;
    private double northBoundary;
    private double eastBoundary;
    private double southBoundary;
    private double westBoundary;
    private boolean enabled;
    private double radius;

    public LoggablePlace() {
        this.radius = 80d;
    }

    public LoggablePlace(Place place) {
        this.id = place.getId();
        this.name = (String) place.getName();
        this.address = (String) place.getAddress();
        this.latitude = place.getLatLng().latitude;
        this.longitude = place.getLatLng().longitude;
        this.northBoundary = place.getViewport().northeast.latitude;
        this.eastBoundary = place.getViewport().northeast.longitude;
        this.southBoundary = place.getViewport().southwest.latitude;
        this.westBoundary = place.getViewport().southwest.longitude;
        this.enabled = true;

        this.radius = 80d;
    }

    public boolean isInPlace(LatLng pos) {
        LatLng myPos = new LatLng(latitude, longitude);
        double dist = FlatEarthDist.distance(myPos, pos);
        Log.d(this.getClass().getSimpleName(), "isInPlace: dist = " + dist + ", radius = " + radius + ", isInPlace = " + (dist < radius));
        return dist < radius;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getNorthBoundary() {
        return northBoundary;
    }

    public void setNorthBoundary(double northBoundary) {
        this.northBoundary = northBoundary;
    }

    public double getEastBoundary() {
        return eastBoundary;
    }

    public void setEastBoundary(double eastBoundary) {
        this.eastBoundary = eastBoundary;
    }

    public double getSouthBoundary() {
        return southBoundary;
    }

    public void setSouthBoundary(double southBoundary) {
        this.southBoundary = southBoundary;
    }

    public double getWestBoundary() {
        return westBoundary;
    }

    public void setWestBoundary(double westBoundary) {
        this.westBoundary = westBoundary;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "LoggablePlace{" +
                "id='" + id + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", name='" + name + '\'' +
                ", northBoundary=" + northBoundary +
                ", eastBoundary=" + eastBoundary +
                ", southBoundary=" + southBoundary +
                ", westBoundary=" + westBoundary +
                ", enabled=" + enabled +
                '}';
    }
}
