package com.trailbook.kole.data;


public class KeyWord {
    public static final int CLIMB = 1;
    public static final int CRAG = 2;
    public static final int REGION = 3;
    public static final int PATH = 4;

    public long _id;
    public String keyWord;
    public int type;
    public String pathId;
    public String pathName;

    public KeyWord(int type, String keyWord, String pathId, String pathName) {
        this.type = type;
        this.keyWord = keyWord;
        this.pathId = pathId;
        this.pathName = pathName;
    }

    public KeyWord() {
    }
}

