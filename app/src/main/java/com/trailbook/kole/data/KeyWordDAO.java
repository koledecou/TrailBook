package com.trailbook.kole.data;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.TrailbookSQLLiteHelper;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KeyWordDAO {

    private static final String CLASS_NAME = "KeyWordDAO";
    // Database fields
    private SQLiteDatabase database;
    private TrailbookSQLLiteHelper dbHelper;
    private String[] allColumns = { TrailbookSQLLiteHelper.COLUMN_ID,
            TrailbookSQLLiteHelper.COLUMN_TYPE,
            TrailbookSQLLiteHelper.COLUMN_PATH_ID,
            TrailbookSQLLiteHelper.COLUMN_PATH_NAME,
            TrailbookSQLLiteHelper.COLUMN_WORD
    };

    private String[] searchManagerColumnNames = new String[] { "_ID",
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA
    };

    public KeyWordDAO(Context context) {
        dbHelper = new TrailbookSQLLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertUpdateKeyWord(KeyWord keyWord) {
        long existingId = getExistingKeywordId(keyWord);
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " existing id " + existingId);
        if (existingId >= 0) {
            return updateKeyWord(existingId, keyWord);
        } else {
            return insertKeyWord(keyWord);
        }
    }

    private long updateKeyWord(long existingId, KeyWord keyWord) {
        ContentValues values = createAllColumnsContentValues(keyWord);

        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " updating " + keyWord.keyWord);
        String selection = TrailbookSQLLiteHelper.COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(keyWord._id) };

        int count = database.update(
                TrailbookSQLLiteHelper.TABLE_KEY_WORDS,
                values,
                selection,
                selectionArgs);

        return existingId;
    }

    private long insertKeyWord(KeyWord keyWord) {
        ContentValues values = createAllColumnsContentValues(keyWord);
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " inserting " + keyWord.keyWord);
        long insertId = database.insert(TrailbookSQLLiteHelper.TABLE_KEY_WORDS, null, values);

        return insertId;
    }

    private long getExistingKeywordId(KeyWord keyWord) {
        long existingId = -1;
        String whereClause = TrailbookSQLLiteHelper.COLUMN_TYPE + " = ? AND " +
                TrailbookSQLLiteHelper.COLUMN_PATH_ID + " = ? AND " +
                TrailbookSQLLiteHelper.COLUMN_WORD + " = ?";
        String[] whereArgs = new String[] {
                Integer.toString(keyWord.type),
                keyWord.pathId,
                keyWord.keyWord
        };
        String orderBy = TrailbookSQLLiteHelper.COLUMN_ID;

        Cursor cursor = database.query(TrailbookSQLLiteHelper.TABLE_KEY_WORDS, allColumns, whereClause, whereArgs,
                null, null, orderBy);
        if (cursor != null && cursor.moveToFirst() ) {
            existingId = cursor.getLong(0);
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " existing Key word found : " + keyWord.keyWord + " path= " + keyWord.pathId + " type= " + keyWord.type);
        }

        cursor.close();
        return existingId;
    }

    private ContentValues createAllColumnsContentValues(KeyWord keyWord) {
        ContentValues values = new ContentValues();
        values.put(TrailbookSQLLiteHelper.COLUMN_TYPE, keyWord.type);
        values.put(TrailbookSQLLiteHelper.COLUMN_PATH_ID, keyWord.pathId);
        values.put(TrailbookSQLLiteHelper.COLUMN_PATH_NAME, keyWord.pathName);
        values.put(TrailbookSQLLiteHelper.COLUMN_WORD, keyWord.keyWord);
        return values;
    }

    public void deleteKeyWord(int type, String pathId, String keyWord) {
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " Key word deleted : " + keyWord + " path= " + pathId + " type= " + type);
        database.delete(TrailbookSQLLiteHelper.TABLE_KEY_WORDS, TrailbookSQLLiteHelper.COLUMN_PATH_ID
                + " = " + pathId +
                " and " + TrailbookSQLLiteHelper.COLUMN_TYPE + " = " + type +
                " and " + TrailbookSQLLiteHelper.COLUMN_WORD + " = " + keyWord, null);
    }

    public ArrayList<KeyWord> getAllKeyWords() {
        ArrayList<KeyWord> keyWords = new ArrayList<KeyWord>();

        Cursor cursor = database.query(TrailbookSQLLiteHelper.TABLE_KEY_WORDS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            KeyWord keyWord = cursorToKeyWord(cursor);
            keyWords.add(keyWord);
            cursor.moveToNext();
        }
        cursor.close();
        return keyWords;
    }

    public ArrayList<KeyWord> getKeyWordsMatching(String search) {
        ArrayList<KeyWord> keyWords = new ArrayList<KeyWord>();

        Cursor cursor = getSuggestions(search);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            KeyWord keyWord = cursorToKeyWord(cursor);
            keyWords.add(keyWord);
            cursor.moveToNext();
        }
        cursor.close();
        return keyWords;
    }

    public Cursor getSuggestions(String search){
        logAllKeywords();
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " getting suggestions for " + search);

        Cursor suggestedPathsCursor = getPathNameMatches(search);
        Cursor suggestedClimbsCursor = getSearchMatches(search, KeyWord.CLIMB);
        Cursor suggestedCragsCursor = getSearchMatches(search, KeyWord.CRAG);
        Cursor suggestedRegionsCursor = getSearchMatches(search, KeyWord.REGION);
        MergeCursor results = new MergeCursor(new Cursor[] {suggestedPathsCursor, suggestedClimbsCursor, suggestedCragsCursor, suggestedRegionsCursor});
        return results;
    }

    private Cursor getPathNameMatches(String search) {
        MatrixCursor mappedResults = new MatrixCursor(allColumns,10);

        Collection<PathSummary> summaries = PathManager.getInstance().getAllSummaries();
        for (PathSummary summary:summaries) {
            String lowerCasePathName = summary.getName().toLowerCase();
            String lowerCaseSearch = search.toLowerCase();
            if (lowerCasePathName.contains(lowerCaseSearch) ) {
                String[] row = new String[]{
                        summary.getId(),
                        Integer.toString(KeyWord.PATH),
                        summary.getId(),
                        summary.getName(),
                        summary.getName()
                };
                mappedResults.addRow(row);
            }
        }

        return mappedResults;
    }

    private void closeCursors(Cursor... cursors) {
        for (Cursor cursor :cursors) {
            cursor.close();
        }
    }

    public Cursor getSuggestionsForSearchManager(String search) {
        Cursor suggestions = getSuggestions(search);
        Cursor suggestionsForSearchManager = mapResultsToSearchManagerColumns(suggestions);
        suggestions.close();
        return suggestionsForSearchManager;
    }

    private Cursor mapResultsToSearchManagerColumns(Cursor... cursors) {
        MatrixCursor mappedResults = new MatrixCursor(searchManagerColumnNames,10);
        for (Cursor c:cursors) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                KeyWord keyWord = cursorToKeyWord(c);
                String[] row;
                row = getSearchResultLabelArray(keyWord);

                mappedResults.addRow(row);
                c.moveToNext();
            }
        }

        return mappedResults;
    }

    private String[] getSearchResultLabelArray(KeyWord keyWord) {
        String[] row;
        String text1, text2;
        if (keyWord.type == KeyWord.PATH) {
            text1 = ApplicationUtils.getLabelForKeywordType(keyWord.type) + keyWord.keyWord;
            text2 = "";
        } else {
            text1 = ApplicationUtils.getLabelForKeywordType(keyWord.type) + keyWord.keyWord;
            text2 = ApplicationUtils.getLabelForKeywordType(keyWord.PATH) + keyWord.pathName;
        }
        return new String[]{
                Long.toString(keyWord._id),
                text1,
                text2,
                keyWord.pathId
        };
    }

    private String getLikeClause(int type) {
        return TrailbookSQLLiteHelper.COLUMN_WORD + " like ? and " +
                TrailbookSQLLiteHelper.COLUMN_TYPE + " = " + Integer.toString(type);
    }

    private Cursor getSearchMatches(String search, int type) {
        String[] args = null;
        String climbsSelection = getLikeClause(type);
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " climbsSelection " + climbsSelection);

        if(search!=null){
            search = "%" + search + "%";
            args = new String[]{search};
        } else {
            return null;
        }

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TrailbookSQLLiteHelper.TABLE_KEY_WORDS);

        Cursor c = queryBuilder.query(database,
                allColumns ,
                climbsSelection,
                args,
                null,
                null,
                TrailbookSQLLiteHelper.COLUMN_WORD + " asc ","10"
        );
        c.moveToFirst();
        while (!c.isAfterLast()) {
            KeyWord keyWord = cursorToKeyWord(c);
            String text1 = ApplicationUtils.getLabelForKeywordType(type) + keyWord.keyWord;
            String text2 = "   Path: " + keyWord.pathName;
            String[] row = new String[] {
                    Long.toString(keyWord._id),
                    text1,
                    text2,
                    keyWord.pathId
            };
            c.moveToNext();
        }
        return c;
    }

    private void logAllKeywords() {
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + "Logging key words ");
        List<KeyWord> words = getAllKeyWords();
        for (KeyWord word:words) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " word " + word.keyWord);
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " type " + word.type);
        }
    }

    private KeyWord cursorToKeyWord(Cursor cursor) {
        KeyWord keyWord = new KeyWord();
        keyWord._id = cursor.getLong(0);
        keyWord.type = cursor.getInt(1);
        keyWord.pathId = cursor.getString(2);
        keyWord.pathName = cursor.getString(3);
        keyWord.keyWord = cursor.getString(4);
        return keyWord;
    }
}
