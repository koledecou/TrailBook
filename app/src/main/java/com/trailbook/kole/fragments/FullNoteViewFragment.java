package com.trailbook.kole.fragments;



import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.trailbook.kole.activities.R;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class FullNoteViewFragment extends Fragment {
    String mNoteID;
    NoteView mNoteView;

    // TODO: Rename and change types of parameters
    public static FullNoteViewFragment newInstance(String noteId) {
        FullNoteViewFragment fragment = new FullNoteViewFragment();
        Bundle args = new Bundle();
        args.putString("note_id", noteId);

        fragment.setArguments(args);
        return fragment;
    }

    public FullNoteViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNoteID = getArguments().getString("note_id");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_full_note, container, false);
        LinearLayout layout = (LinearLayout)view.findViewById(R.id.fnv_layout);
        mNoteView = (FullNoteView)layout.findViewById(R.id.frag_note_view);
//        mNoteView = getNoteView(mNoteID);
//        layout.addView(mNoteView);
//        mNoteView = (NoteView)view.findViewById(R.id.frag_note_view);
        mNoteView.setNoteId(mNoteID);

        return view;
    }

    private NoteView getNoteView(String noteId) {
        NoteView nv = new FullNoteView(getActivity());
        nv.setNoteId(noteId);
        return nv;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
