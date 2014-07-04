package com.trailbook.kole.data;

public abstract class Note {
    public long id;
    public String content;
    public String imageFileName;

    public void setNoteContent(String content) {
        this.content=content;
    }
    public void setImageFileName(String imageFileName) {
        this.imageFileName=imageFileName;
    }
    public void setId(long id) {
        this.id=id;
    }

    public String getNoteContent() {
        return content;
    }
    public long getNoteID() {
        return id;
    }
    public String getImageFileName() {
        return imageFileName;
    }
}