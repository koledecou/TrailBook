package com.trailbook.kole.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.fragments.point_attched_object_view.PointAttachedObjectView;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.location_processors.PathFollowerLocationProcessor;
import com.trailbook.kole.state_objects.PathManager;

public class ApproachingObjectNotificationReceiverActivity extends Activity {
    String mId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            mId = extras.getString(PathFollowerLocationProcessor.EXTRA_OBJECT_ID);
            Log.d(Constants.TRAILBOOK_TAG, "NoteNotificationReceiverActivity: NoteId=" + mId);
        }
        PointAttachedObject paObject = PathManager.getInstance().getPointAttachedObject(mId);
//        setContentView(NoteFactory.getFullScreenFragmentLayoutId(paObject.getAttachment().getType()));
        setContentView(R.layout.fragment_full_pao);

        LinearLayout layout = (LinearLayout)findViewById(R.id.full_pao_layout);
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": inflating R.layout.view_note_full");
        PointAttachedObjectView view = NoteFactory.getFullScreenView(paObject);
        view.setPaoId(mId);
        layout.addView(view);
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
