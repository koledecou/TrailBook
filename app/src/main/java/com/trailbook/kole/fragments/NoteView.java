package com.trailbook.kole.fragments;

import android.content.Context;
import android.media.Image;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.ButtonActions;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.tools.PathManager;
import com.trailbook.kole.tools.TrailbookFileUtilities;

public class NoteView extends LinearLayout {
    private PathManager mPathManager;

    private String mNoteId = "1";
    private String mContent = "This is a sample path";
    private String mImageFileName = null;
    private Image mImage = null;
    private TextView mTextViewContent;
    private TextView mTextViewLocationInfo;
    private ImageView mImageView;

    public NoteView(Context context) {
        super(context);
        loadViews();
    }

    public NoteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadViews();
    }

    public NoteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadViews();
    }

    public void setNoteId(String noteId) {
        mNoteId=noteId;
        mPathManager = PathManager.getInstance();
        PointAttachedObject<Note> paoNote = mPathManager.getNote(noteId);
        if (paoNote == null)
            return;

        Note note = paoNote.getAttachment();
        mContent=note.getNoteContent();
        if (mTextViewContent != null)
            mTextViewContent.setText(mContent);

        mImageFileName = note.getImageFileName();
        if (mImageFileName != null && mImageFileName.length()>0) {
            String segmentId = note.getParentSegmentId();
            Log.d(Constants.TRAILBOOK_TAG, "loading image :" + mImageFileName);
            Picasso.with(getContext()).load(TrailbookFileUtilities.getInternalImageFile(getContext(), segmentId, mImageFileName)).into(mImageView);
        }

        mTextViewLocationInfo.setText("This note is {distance} {units} {direction} of you");
    }

    private void loadViews(){
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        inflater.inflate(R.layout.view_note, this);

        mTextViewContent=(TextView)findViewById(R.id.vn_text_content);
        mImageView=(ImageView)findViewById(R.id.vn_image);
        mTextViewLocationInfo=(TextView)findViewById(R.id.vn_navigation_details);
    }
}
