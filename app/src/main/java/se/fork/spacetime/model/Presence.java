package se.fork.spacetime.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by per.fork on 2018-02-03.
 */

public class Presence implements Parcelable {

    private List<String> presentPlaces;

    protected Presence(Parcel in) {
        presentPlaces = in.createStringArrayList();
    }

    public Presence(List<String> presentPlaces) {
        this.presentPlaces = presentPlaces;
    }

    public static final Creator<Presence> CREATOR = new Creator<Presence>() {
        @Override
        public Presence createFromParcel(Parcel in) {
            return new Presence(in);
        }

        @Override
        public Presence[] newArray(int size) {
            return new Presence[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(presentPlaces);
    }

    public List<String> getPresentPlaces() {
        return presentPlaces;
    }

    public void setPresentPlaces(List<String> presentPlaces) {
        this.presentPlaces = presentPlaces;
    }

    @Override
    public String toString() {
        return "Presence{" +
                "presentPlaces=" + presentPlaces +
                '}';
    }
}
