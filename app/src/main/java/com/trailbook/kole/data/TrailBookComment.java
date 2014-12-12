package com.trailbook.kole.data;

import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.helpers.TrailbookPathUtilities;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by kole on 11/29/2014.
 */
public class TrailBookComment implements Attachment {
    public String comment;
    public User user;
    public String pathId;
    public String _id;
    public ArrayList<String> imageFileNames;
    public long timeStamp;

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public TrailBookComment(String pathId, String comment, User user) {
        this.user=user;
        this.comment = comment;
        this.pathId = pathId;
        this._id = TrailbookPathUtilities.getNewCommentId();
        this.timeStamp = new Date().getTime();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }

    @Override
    public String getType() {
        return NoteFactory.COMMENT;
    }

    @Override
    public ArrayList<String> getImageFileNames() {
        return imageFileNames;
    }

    @Override
    public String getShortContent() {
        return comment;
    }

    @Override
    public String getNotificationString() {
        return comment;
    }

    @Override
    public void addImageFiles(ArrayList<String> imageFileNames) {
        this.imageFileNames = imageFileNames;
    }
}
