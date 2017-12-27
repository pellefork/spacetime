package se.fork.spacetime.model;

import com.google.android.gms.location.places.Place;

/**
 * Created by per.fork on 2017-12-27.
 */

public class LoggablePlace {
    private Place place;
    private double detectableRadius;
    private boolean enabled;

    public LoggablePlace() {
    }

    public LoggablePlace(Place place) {
        this.place = place;
        this.enabled = true;
    }

    public double getDetectableRadius() {
        return detectableRadius;
    }

    public void setDetectableRadius(double detectableRadius) {
        this.detectableRadius = detectableRadius;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
