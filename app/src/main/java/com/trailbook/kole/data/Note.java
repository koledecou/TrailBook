package com.trailbook.kole.data;

import com.trailbook.kole.helpers.NoteFactory;

public class Note implements Attachment {
    public String content;
    public String imageFileName;

    public Note() {
    }

    public void setNoteContent(String content) {
        this.content=content;
    }
    public void setImageFileName(String imageFileName) {
        this.imageFileName=imageFileName;
    }

    public String getNoteContent() {
        return content;
    }

    @Override
    public String getType() {
        return NoteFactory.NOTE;
    }

    @Override
    public String getImageFileName() {
        return imageFileName;
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