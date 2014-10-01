package com.trailbook.kole.fragments.point_attched_object_view.climb;

import android.content.Context;
import android.util.AttributeSet;

import com.trailbook.kole.activities.R;

/**
 * Created by kole on 9/4/2014.
 */
public class FullClimbView extends ClimbView {
    public FullClimbView(Context context) {
        super(context);
//        init( R.layout.view_note_full, R.id.nv_full_note_layout);
        init( R.layout.view_climb_full, -1);
    }

    public FullClimbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init( R.layout.view_climb_full, -1);
    }

    public FullClimbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init( R.layout.view_climb_full, -1);
    }
}
