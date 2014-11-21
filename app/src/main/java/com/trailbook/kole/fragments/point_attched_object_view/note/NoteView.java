package com.trailbook.kole.fragments.point_attched_object_view.note;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.fragments.point_attched_object_view.PointAttachedObjectView;

public class NoteView extends PointAttachedObjectView {
    String mContent = "This is a sample path";

    TextView mTextViewContent;

    public NoteView(Context context) {
        super(context);
    }

    public NoteView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void populateFieldsFromObject(PointAttachedObject pao) {
        super.populateFieldsFromObject(pao);
        Note note = (Note)pao.getAttachment();
        mContent=note.getNoteContent();
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": populating note content " + mContent);
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": text view is " + mTextViewContent);
        if (mTextViewContent != null)
            mTextViewContent.setText(mContent);
    }



    @Override
    public void loadViews(){
        super.loadViews();
        mTextViewContent=(TextView)findViewById(R.id.vn_text_content);
    }
}
