package com.trailbook.kole.helpers;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class PathExclusionStrategy implements ExclusionStrategy {

    public boolean shouldSkipClass(Class<?> arg0) {
        return false;
    }

    public boolean shouldSkipField(FieldAttributes f) {
        if (f.getDeclaringClass() == LatLng.class) {
            return shouldSkipLatLonFiled(f.getName());
        }

        return false;
    }

    private boolean shouldSkipLatLonFiled(String fieldName) {
        if (fieldName.equals("latitude") || fieldName.equals("longitude")) {
            return false;
        } else {
            return true;
        }
    }
}