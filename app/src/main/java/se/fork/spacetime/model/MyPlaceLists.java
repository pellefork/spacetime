package se.fork.spacetime.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by per.fork on 2017-12-27.
 */

public class MyPlaceLists {
    private List<String> keys;

    public MyPlaceLists() {
        keys = new ArrayList<>();
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public void addKey(String key) {
        keys.add(key);
    }
}
