package com.trailbook.kole.fragments.point_attched_object_view.climb;

import android.content.Context;
import android.util.AttributeSet;

import com.trailbook.kole.activities.R;

/**
 * Created by kole on 9/29/2014.
 */
public class SmallClimbView extends ClimbView{
    public SmallClimbView(Context context) {
        super(context);
        init( R.layout.view_climb_small, R.id.vc_small_climb_layout);
    }

    public SmallClimbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init( R.layout.view_climb_small, R.id.vc_small_climb_layout);
    }

    public SmallClimbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init( R.layout.view_climb_small, R.id.vc_small_climb_layout);
    }
}
