package com.trailbook.kole.data.geo;

import com.google.android.gms.maps.model.LatLng;

/**
 * An GEOJSON object for use with native geo location support in Mongodb.
 * See spec at http://geojson.org/geojson-spec.html#examples
 */
public class GeoPoint {
    private String type = "Point";
    private double[] coordinates;

    public GeoPoint() {
    }

    public GeoPoint(double longitude, double latitude) {
        coordinates = new double[]{longitude, latitude};
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    public LatLng toLatLng() {
        if (coordinates != null && coordinates.length > 1)
            return new LatLng(coordinates[1], coordinates[0]);
        else
            return null;
    }
}

