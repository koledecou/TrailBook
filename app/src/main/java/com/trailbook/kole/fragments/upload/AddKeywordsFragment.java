package com.trailbook.kole.fragments.upload;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.Constants;

import java.util.ArrayList;

public class AddKeywordsFragment extends Fragment implements View.OnClickListener {

    private static final String KEYWORDS_KEY = "KEY_WORDS";
    private static final String TYPE_KEY = "TYPE";
    private static final String PATH_ID_KEY = "PATH_ID";

    public interface KeywordsAddedListener {
        public void keywordsAdded(String pathId, ArrayList<String> keyWords, int type);
    }

    public static final int TYPE_CLIMB = 1;
    public static final int TYPE_CRAG = 2;
    public static final int TYPE_REGION = 3;
    public static final String TYPE = "TYPE";
    public static final String PATH_ID = "PATH_ID";
    private static final int N_ITEMS = 10;
    private static final String CLASSNAME = "AddKeywordsFragment";
    private ArrayList<EditText> mEditTextArrayKeywords;

    private static KeywordsAddedListener mListener;
    private int mType;
    private String mPathId;

    public AddKeywordsFragment() {
        // Empty constructor required for DialogFragment
    }

    public static AddKeywordsFragment newInstance(int type, String pathId) {
        AddKeywordsFragment frag = new AddKeywordsFragment();
        Bundle args = new Bundle();
        args.putInt(TYPE, type);
        args.putString(PATH_ID, pathId);
        frag.setArguments(args);
        return frag;
    }

    public void setListener(KeywordsAddedListener listener) {
        mListener = listener;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            FragmentManager fm = activity.getFragmentManager();
            Fragment pathUploadDetailsFragment = fm.findFragmentByTag(TrailBookActivity.PATH_DETAILS_DIALOG_TAG);
            mListener = (KeywordsAddedListener) pathUploadDetailsFragment;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> words = new ArrayList<String>();
        if (mEditTextArrayKeywords != null) {
            for (EditText et:mEditTextArrayKeywords) {
                String word = et.getText().toString();
                words.add(word);
            }
            outState.putStringArrayList(KEYWORDS_KEY, words);
        }
        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + ": Saved key words");

        outState.putString(PATH_ID_KEY, mPathId);
        outState.putInt(TYPE_KEY, mType);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mType = getArguments().getInt(TYPE);
        mPathId = getArguments().getString(PATH_ID);

        View view  = inflater.inflate(R.layout.add_keywords, container, false);

        TextView titleView = (TextView)view.findViewById(R.id.title);
        setTitleText(titleView, mType);

        LinearLayout listContainer = (LinearLayout) view.findViewById(R.id.edit_text_container);
        AddKeywordEditTextsToContainer(listContainer, savedInstanceState);

        setButtons(view);

        return view;
    }

    private void AddKeywordEditTextsToContainer(LinearLayout container, Bundle savedInstanceState) {
        ArrayList<String> savedValues = new ArrayList<String>();
        if (savedInstanceState  != null) {
            savedValues = savedInstanceState.getStringArrayList(KEYWORDS_KEY);
        }
        mEditTextArrayKeywords = new ArrayList<EditText>();
        for (int i=1; i <= N_ITEMS; i++) {
            View itemView = getItemView(i);
            container.addView(itemView);
            if (savedValues != null && savedValues.size() >= i) {
                EditText et = mEditTextArrayKeywords.get(i-1);
                et.setText(savedValues.get(i-1));
            }
        }


    }

    private View getItemView(int itemNumber) {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView tv = new TextView(getActivity());
        tv.setText(String.valueOf(itemNumber) + ".");
        EditText et = new EditText(getActivity());
        et.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        layout.addView(tv);
        layout.addView(et);

        mEditTextArrayKeywords.add(et);

        return layout;
    }

    private void setTitleText(TextView titleView, int type) {
        if (type == TYPE_CLIMB) {
            titleView.setText(getString(R.string.keyword_title_climbs));
        } else if (type == TYPE_CRAG) {
            titleView.setText(getString(R.string.keyword_title_crags));
        } else if (type == TYPE_REGION) {
            titleView.setText(getString(R.string.keyword_title_regions));
        }
    }

    private void setButtons(View v) {
        Button cancel = (Button)v.findViewById(R.id.b_cancel);
        Button okButton = (Button) v.findViewById(R.id.b_ok);

        cancel.setOnClickListener(this);
        okButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.b_ok) {
            ArrayList<String> keyWords = new ArrayList<String>();
            for (EditText et:mEditTextArrayKeywords) {
                String word = et.getText().toString();
                if (word != null && word.trim().length()>0) {
                    Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + " adding keyword " + word);
                    keyWords.add(word);
                }
            }
            mListener.keywordsAdded(mPathId, keyWords, mType);
            getFragmentManager().popBackStackImmediate();
        } else if (v.getId() == R.id.b_cancel) {
            getFragmentManager().popBackStackImmediate();
        }
    }

}