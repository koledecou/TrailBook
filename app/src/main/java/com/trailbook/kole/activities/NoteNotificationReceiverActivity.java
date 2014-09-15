package com.trailbook.kole.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.fragments.FullNoteView;
import com.trailbook.kole.location_processors.PathFollowerLocationProcessor;

public class NoteNotificationReceiverActivity extends Activity {

    FullNoteView mNoteView;
    String mNoteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            mNoteId = extras.getString(PathFollowerLocationProcessor.EXTRA_NOTE_ID);
            Log.d(Constants.TRAILBOOK_TAG, "NoteNotificationReceiverActivity: NoteId=" + mNoteId);
        }

        setContentView(R.layout.fragment_full_note);

        mNoteView = (FullNoteView)findViewById(R.id.frag_note_view);
        mNoteView.setNoteId(mNoteId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.notification_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
