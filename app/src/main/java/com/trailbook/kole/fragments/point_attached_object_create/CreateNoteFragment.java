package com.trailbook.kole.fragments.point_attached_object_create;

import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Attachment;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.state_objects.PathManager;

public class CreateNoteFragment extends CreatePointAttachedObjectFragment {
    private static final String TEXT =  "TEXT";
    private EditText mEditTextContent;

    public static CreateNoteFragment newInstance(String noteId) {
        CreateNoteFragment fragment = new CreateNoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    public CreateNoteFragment() {
        super();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mEditTextContent.getText() != null)
            outState.putString(TEXT, mEditTextContent.getText().toString());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflateView(inflater, container, R.layout.create_note);
        createViewObjects();
        mEditTextContent = (EditText) view.findViewById(R.id.cn_note_content);

        if (savedInstanceState == null) {
            populateValues(PathManager.getInstance().getPointAttachedObject(mNoteId));
        } else {
            restoreInstance(savedInstanceState);
        }

        return view;
    }

    @Override
    public void populateValues(PointAttachedObject paoNote) {
        super.populateValues(paoNote);
        if (paoNote != null) {
            Note note = (Note)paoNote.getAttachment();
            String noteContent = note.getNoteContent();
            mEditTextContent.setText(noteContent);
        }
    }

    @Override
    protected void restoreInstance(Bundle savedInstanceState) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() +"restoring note state");
        super.restoreInstance(savedInstanceState);
        if (savedInstanceState != null) {
            String text = savedInstanceState.getString(TEXT);
            mEditTextContent.setText(text);
        }
    }

    protected Attachment createAttachment() {
        Note newNote = new Note();
        newNote.setNoteContent(mEditTextContent.getText().toString());
        return newNote;
    }

    @Override
    protected void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditTextContent.getWindowToken(), 0);
    }

    @Override
    protected void showSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditTextContent, 0);
    }
}
