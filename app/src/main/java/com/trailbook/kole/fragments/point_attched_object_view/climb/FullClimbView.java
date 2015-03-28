package com.trailbook.kole.fragments.point_attched_object_view.climb;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Climb;
import com.trailbook.kole.data.PointAttachedObject;

import java.util.ArrayList;

/**
 * Created by kole on 9/4/2014.
 */
public class FullClimbView extends ClimbView {
    TextView mTextViewPitchCount;
    TextView mTextViewCategory;
    TextView mTextViewRackDescription;
    LinearLayout mPitchDescriptionContainer;

    public FullClimbView(Context context) {
        super(context);
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


    @Override
    public void populateFieldsFromObject(PointAttachedObject pao) {
        super.populateFieldsFromObject(pao);
        Climb climb = (Climb)pao.getAttachment();
        String pitchCount = String.valueOf(climb.getPitchCount());
        mTextViewPitchCount.setText(pitchCount);
        mTextViewCategory.setText(climb.getClimbType());
        mTextViewRackDescription.setText(climb.getRackDescription());
        //addSamplePitchDescriptions(climb);
        buildPitchDescriptions(climb.getPitchDescriptions());
    }

    private void addSamplePitchDescriptions(Climb climb) {
        climb.addPitchDescription("thin hands 5.10- to choss to ledge with bad bolts");
        climb.addPitchDescription("5.10+ Enduro OW chickenwing/hand stack or lieback 150ft or so to some truly bad rusty button head bolts at the notch.");
        climb.addPitchDescription("5.5 climb choss to tunnel thru to easy climbing to a spectacular summit.");
        climb.addPitchDescription("you're already on the top, there are no more pitches :).");
    }

    private void buildPitchDescriptions(ArrayList<String> pitchDescriptions) {
        mPitchDescriptionContainer.removeAllViews();
        if (pitchDescriptions != null) {
            int pitchNum=1;
            for (String description:pitchDescriptions) {
                LinearLayout layout = createPitchDescriptionView(pitchNum, description);

                mPitchDescriptionContainer.addView(layout);
                pitchNum++;
            }
        }
    }

    private LinearLayout createPitchDescriptionView(int pitchNum, String description){
        RelativeLayout.LayoutParams params=new RelativeLayout.LayoutParams
                ((int)LayoutParams.WRAP_CONTENT,(int)LayoutParams.WRAP_CONTENT);

        LinearLayout layout = new LinearLayout(this.getContext());

        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView tvLabel = getPitchNumberTextView(pitchNum, params);
        layout.addView(tvLabel);

        TextView tvDescription = getPitchDescriptionTextView(description, params);
        layout.addView(tvDescription);

        return layout;
    }

    private TextView getPitchDescriptionTextView(String description, RelativeLayout.LayoutParams params) {
        TextView tvDescription = new TextView(this.getContext());
        tvDescription.setText(description);
        tvDescription.setTextColor(Color.BLACK);
        tvDescription.setLayoutParams(params);
        return tvDescription;
    }

    private TextView getPitchNumberTextView(int pitchNum, RelativeLayout.LayoutParams params) {
        TextView tvLabel=new TextView(this.getContext());
        tvLabel.setText("Pitch " + pitchNum + " ");
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(Color.BLACK);
        tvLabel.setLayoutParams(params);
        return tvLabel;
    }

    @Override
    public void loadViews(){
        super.loadViews();

        mTextViewPitchCount=(TextView)findViewById(R.id.vc_pitch_count);
        mTextViewCategory=(TextView)findViewById(R.id.vc_climb_category);
        mTextViewRackDescription=(TextView)findViewById(R.id.vc_rack);
        mPitchDescriptionContainer = (LinearLayout)findViewById(R.id.vc_pitch_description_container);
    }
}
