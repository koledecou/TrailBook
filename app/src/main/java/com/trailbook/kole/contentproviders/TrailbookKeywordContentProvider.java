package com.trailbook.kole.contentproviders;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.KeyWordDAO;
import com.trailbook.kole.helpers.TrailbookSQLLiteHelper;


public class TrailbookKeywordContentProvider extends ContentProvider {
    private static final String CLASS_NAME = "TrailbookKeywordContentProvider";
    private TrailbookSQLLiteHelper mDatabaseHelper;

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new TrailbookSQLLiteHelper(getContext());
        return true;
    }

    // Implements ContentProvider.query()
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " uri is " + uri);
        String query = uri.getLastPathSegment().toLowerCase();
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " getting suggestions for " + query);

        if (SearchManager.SUGGEST_URI_PATH_QUERY.equals(query)) {
            return null;
        }
        else {
            KeyWordDAO keyWordDAO = new KeyWordDAO(getContext());
            keyWordDAO.open();
            Cursor c = null;
            try {
                c = keyWordDAO.getSuggestionsForSearchManager(query);
            }catch (Exception e) {
                Log.e(Constants.TRAILBOOK_TAG, CLASS_NAME + " exception getting suggestion", e);
            } finally {
                keyWordDAO.close();
            }
            return c;
        }
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}