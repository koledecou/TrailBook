package com.trailbook.kole.data;

import com.google.gson.annotations.Expose;

public class Note {
    public String noteId;
    public String content;
    public String imageFileName;
    public String parentPathId;

    public Note() {}
    public Note(String noteId, String parentPathId) {
        this.noteId=noteId;
        this.parentPathId = parentPathId;
    }

    public void setParentPathId(String parentPathId) {
        this.parentPathId = parentPathId;
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

    public String getParentPathId() {
        return parentPathId;
    }
}