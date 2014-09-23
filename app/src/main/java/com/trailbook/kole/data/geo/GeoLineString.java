package com.trailbook.kole.data.geo;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * An GEOJSON object for use with native geo location support in Mongodb.
 * See spec at http://geojson.org/geojson-spec.html#examples
 */
public class GeoLineString {
    private String type = "LineString";
    private ArrayList<double[]> coordinates;

    public ArrayList<double[]> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(ArrayList<double[]> coordinates) {
        this.coordinates = coordinates;
    }

    public ArrayList<LatLng> toLatLngArray() {
        ArrayList<LatLng> points = new ArrayList<LatLng>();
        if (coordinates != null) {
            for (double[] point: coordinates) {
                LatLng latLngPoint = new LatLng(point[1], point[0]);
                points.add(latLngPoint);
            }
            return points;
        } else {
            return null;
        }
    }

    public int size() {
        if (coordinates == null)
            return 0;
        else
            return coordinates.size();
    }

    public void add(LatLng newPoint) {
        double[] point = new double[] {newPoint.longitude, newPoint.latitude};
        if (coordinates == null)
            coordinates = new ArrayList<double[]>();

        coordinates.add(point);
    }

    public LatLng getLast() {
        if (coordinates.size()>0) {
            double[] p = coordinates.get(coordinates.size() - 1);
            return new LatLng(p[1], p[0]);
        } else {
            return null;
        }
    }

    public void addAll(ArrayList<LatLng> newPoints) {
        for (LatLng p:newPoints) {
            add(p);
        }
    }
}