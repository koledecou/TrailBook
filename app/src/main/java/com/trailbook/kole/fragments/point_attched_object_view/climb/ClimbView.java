package com.trailbook.kole.fragments.point_attched_object_view.climb;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Climb;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.fragments.point_attched_object_view.PointAttachedObjectView;

public class ClimbView extends PointAttachedObjectView {
    TextView mTextViewName;
    TextView mTextViewGrade;
    TextView mTextViewDescription;
    //todo: rack, type, length, etc...

    public ClimbView(Context context) {
        super(context);
    }

    public ClimbView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClimbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void populateFieldsFromObject(PointAttachedObject pao) {
        super.populateFieldsFromObject(pao);
        Climb climb = (Climb)pao.getAttachment();
        mTextViewName.setText(climb.getName());
        mTextViewGrade.setText(climb.getGrade().grade);
        mTextViewDescription.setText(climb.getDescription());
    }

    @Override
    public void loadViews(){
        super.loadViews();

        mTextViewName=(TextView)findViewById(R.id.vc_name);
        mTextViewGrade=(TextView)findViewById(R.id.vc_grade);
        mTextViewDescription=(TextView)findViewById(R.id.vc_description);
    }
}
