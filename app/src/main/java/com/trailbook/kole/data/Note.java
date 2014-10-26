package com.trailbook.kole.data;

import com.trailbook.kole.helpers.NoteFactory;

import java.util.ArrayList;

public class Note implements Attachment {
    public String content;
    public ArrayList<String> imageFileNames;
    //public String imageFileName;

    public Note() {
        this.imageFileNames = new ArrayList<String>();
    }

    public void setNoteContent(String content) {
        this.content=content;
    }

    public void addImageFile(String fileName) {
        imageFileNames.add(fileName);
    }

    public void addImageFiles(ArrayList<String> fileNames) {
        imageFileNames.addAll(fileNames);
    }

    public String getNoteContent() {
        return content;
    }

    @Override
    public String getType() {
        return NoteFactory.NOTE;
    }

    @Override
    public ArrayList<String> getImageFileNames() {
        //todo: remove
/*        if (imageFileName != null) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": legacy image file not null.  adding " + imageFileName);
            if (imageFileNames.size() == 0) {
                imageFileNames.add(imageFileName);
            }

        }*/

        if (imageFileNames.size()>0)
            return imageFileNames;
        else
            return null;
    }

    @Override
    public String getShortContent() {
        return content;
    }

    @Override
    public String getNotificationString() {
        return content;
    }

    public String toString() {
        return content;
    }
}