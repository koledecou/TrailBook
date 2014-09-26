package com.trailbook.kole.data;

import java.util.ArrayList;

/**
 * Created by kole on 9/24/2014.
 */
public class Climb {
    public String _id;
    public String name;
    public Grade grade;
    public ArrayList<String> imageFileNames;
    public String description;

    public Climb(String id, String name) {
        this._id = id;
        this.name = name;
        this.imageFileNames = new ArrayList<String>();
    }

    public void addImageFile(String fileName) {
        imageFileNames.add(fileName);
    }
}
