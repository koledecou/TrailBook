package com.trailbook.kole.fragments;

import android.content.Context;
import android.util.AttributeSet;

import com.trailbook.kole.activities.R;

/**
 * Created by kole on 9/4/2014.
 */
public class FullNoteView extends NoteView {
    public FullNoteView(Context context) {
        super(context);
//        init( R.layout.view_note_full, R.id.nv_full_note_layout);
        init( R.layout.view_note_full, -1);
    }

    public FullNoteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init( R.layout.view_note_full, -1);
    }

    public FullNoteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init( R.layout.view_note_full, -1);
    }
}
