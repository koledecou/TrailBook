package com.trailbook.kole.tools;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class PathExclusionStrategy implements ExclusionStrategy {

    public boolean shouldSkipClass(Class<?> arg0) {
        return false;
    }

    public boolean shouldSkipField(FieldAttributes f) {
        if (f.getDeclaringClass() == LatLng.class) {
            if (f.getName().equals("latitude") ||
                f.getName().equals("longitude")) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }
}