package com.trailbook.kole.fragments.point_attched_object_view.note;

import android.content.Context;
import android.util.AttributeSet;

import com.trailbook.kole.activities.R;

/**
 * Created by kole on 9/4/2014.
 */
public class SmallNoteView extends NoteView {

    public SmallNoteView(Context context) {
        super(context);
        init( R.layout.view_note_small, R.id.nv_small_note_layout);
    }

    public SmallNoteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init( R.layout.view_note_small, R.id.nv_small_note_layout);
    }

    public SmallNoteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init( R.layout.view_note_small, R.id.nv_small_note_layout);
    }
}
