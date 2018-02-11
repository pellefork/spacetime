package se.fork.spacetime.model;

import android.os.Parcelable;
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
    private boolean enabled;
    private double radius;
    private boolean inside;

    public LoggablePlace() {
        this.radius = 80d;
    }

    public LoggablePlace(Place place) {
        this.id = place.getId();
        this.name = (String) place.getName();
        this.address = (String) place.getAddress();
        this.latitude = place.getLatLng().latitude;
        this.longitude = place.getLatLng().longitude;
        this.enabled = true;
        this.radius = 80d;
        this.inside = false;
    }

    public boolean isInGivenPosition(LatLng pos) {
        LatLng myPos = new LatLng(latitude, longitude);
        double dist = FlatEarthDist.distance(myPos, pos);
        Log.d(this.getClass().getSimpleName(), "isInGivenPosition: dist = " + dist + ", radius = " + radius + ", isInGivenPosition = " + (dist < radius));
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public boolean isInside() {
        return inside;
    }

    public void setInside(boolean inside) {
        this.inside = inside;
    }

    @Override
    public String toString() {
        return "LoggablePlace{" +
                "id='" + id + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", enabled=" + enabled +
                ", radius=" + radius +
                ", inside=" + inside +
                '}';
    }
}
