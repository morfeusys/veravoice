package com.example.vera;

import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by morfeusys on 19.09.14.
 */
public class Device implements Comparable<Device> {

    public static enum Category {
        Switch, Dimmer, Thermostat("temperature");

        public final String field;

        Category(String field) {
            this.field = field;
        }

        Category() {
            this(null);
        }
    }

    public final int id;
    public final String name;
    public final Category category;

    public final Map<String, Object> data = new HashMap<String, Object>();

    public Device(int id, String name, Category category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    @Override
    public int compareTo(Device another) {
        return another.name.length() - this.name.length();
    }

    public static Device fromJSON(JSONObject json, SparseArray<String> categories) {
        try {
            String cat = categories.get(json.getInt("category"));
            if (cat == null) return null;
            Category category = Category.valueOf(cat);
            Device device = new Device(json.getInt("id"), json.getString("name").toLowerCase(), category);
            JSONArray names = json.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                device.data.put(name, json.get(name));
            }
            return device;
        } catch (Exception e) {
            return null;
        }
    }
}
