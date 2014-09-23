package com.trailbook.kole.data;

public class Note {
    public String content;
    public String imageFileName;

    public Note() {}

    public void setNoteContent(String content) {
        this.content=content;
    }
    public void setImageFileName(String imageFileName) {
        this.imageFileName=imageFileName;
    }

    public String getNoteContent() {
        return content;
    }
    public String getImageFileName() {
        return imageFileName;
    }
}