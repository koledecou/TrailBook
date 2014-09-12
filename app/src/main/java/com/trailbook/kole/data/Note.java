package com.trailbook.kole.data;

import com.google.gson.annotations.Expose;

public class Note {
    public String noteId;
    public String content;
    public String imageFileName;
    public String parentSegmentId;

    public Note() {}
    public Note(String noteId, String parentSegmentId) {
        this.noteId=noteId;
        this.parentSegmentId = parentSegmentId;
    }

    public void setParentSegmentId(String parentSegmentId) {
        this.parentSegmentId = parentSegmentId;
    }

    public void setNoteContent(String content) {
        this.content=content;
    }
    public void setImageFileName(String imageFileName) {
        this.imageFileName=imageFileName;
    }
    public void setId(String id) {
        this.noteId=id;
    }

    public String getNoteContent() {
        return content;
    }
    public String getNoteID() {
        return noteId;
    }
    public String getImageFileName() {
        return imageFileName;
    }

    public String getParentSegmentId() {
        return parentSegmentId;
    }
}