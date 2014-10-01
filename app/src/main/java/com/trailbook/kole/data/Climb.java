package com.trailbook.kole.data;

import com.trailbook.kole.helpers.NoteFactory;

import java.util.ArrayList;

/**
 * Created by kole on 9/24/2014.
 */
public class Climb implements Attachment {
    public String name;
    public Grade grade;
    public String description;
    public String rackDescription;
    public String climbType;

    public ArrayList<String> imageFileNames;

    public Climb() {
        this.imageFileNames = new ArrayList<String>();
    }

    public String getRackDescription() {
        return rackDescription;
    }

    public void setRackDescription(String rackDescription) {
        this.rackDescription = rackDescription;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addImageFile(String fileName) {
        imageFileNames.add(fileName);
    }

    public String toString() {
        return name;
    }

    public void setClimbType(String climbType) {
        this.climbType = climbType;
    }

    public String getClimbType() {
        return climbType;
    }

    @Override
    public String getType() {
        return NoteFactory.CLIMB;
    }

    @Override
    public String getImageFileName() {
        if (imageFileNames.size()>0)
            return imageFileNames.get(0);
        else
            return null;
    }

    @Override
    public String getShortContent() {
        return name;
    }

    @Override
    public String getNotificationString() {
        return name;
    }
}
