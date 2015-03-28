package com.trailbook.kole.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TrailbookSQLLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_KEY_WORDS = "key_words";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_WORD = "word";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_PATH_ID = "path_id";
    public static final String COLUMN_PATH_NAME = "path_name";

    private static final String DATABASE_NAME = "trailbook.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_KEY_WORDS + "(" + COLUMN_ID
            + " integer primary key autoincrement, "
            + COLUMN_WORD + " text not null, "
            + COLUMN_TYPE + " integer not null, "
            + COLUMN_PATH_ID + " text not null, "
            + COLUMN_PATH_NAME + " text not null "
            + ");";

    public TrailbookSQLLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEY_WORDS);
        onCreate(db);
    }

}