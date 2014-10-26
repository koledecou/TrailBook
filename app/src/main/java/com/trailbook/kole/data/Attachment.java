package com.trailbook.kole.data;

import java.util.ArrayList;

/**
 * Created by kole on 9/25/2014.
 */
public interface Attachment {
    public String getType();
    public ArrayList<String> getImageFileNames();
    public String getShortContent();
    public String getNotificationString();
    public void addImageFiles(ArrayList<String> mImageFileNames);
}
