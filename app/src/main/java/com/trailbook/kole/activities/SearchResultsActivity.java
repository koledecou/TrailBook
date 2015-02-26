package com.trailbook.kole.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.KeyWord;
import com.trailbook.kole.data.KeyWordDAO;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

import java.util.ArrayList;

public class SearchResultsActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final String CLASS_NAME = "SearchResultsActivity";
    private ArrayList<KeyWord> mResults;
    private KeyWordArrayAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " creating context menu");
        if (v.getId() == R.id.lv_results) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            KeyWord keyWord = (KeyWord)mAdapter.getItem(info.position);
            PathSummary summary = PathManager.getInstance().getPathSummary(keyWord.pathId);
            menu.setHeaderTitle(summary.getName());
            ApplicationUtils.addPathActionMenuItemsForSearchResults(menu, keyWord.pathId);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " clicked on item ");

        ContextMenu.ContextMenuInfo menuInfo = item.getMenuInfo();
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        KeyWord keyWord = (KeyWord)mAdapter.getItem(info.position);
        PathSummary summary = PathManager.getInstance().getPathSummary(keyWord.pathId);

        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " need to launch main activity for action " + summary.getName() + " " + item.getItemId());
        launchMapForPath(summary.getId());
        return true;
    }

    private void handleIntent(Intent intent) {
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " action " + intent.getAction());
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " searching for " + query);
            mResults = getSearchResults(query);
            displayResults(mResults);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String pathId = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " path id is " + pathId);
            launchMapForPath(pathId);
        }
    }

    private void launchMapForPath(String pathId) {
/*        Intent launchTrailbookIntent = new Intent(this, TrailBookActivity.class);
        launchTrailbookIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        launchTrailbookIntent.putExtra(TrailBookActivity.INITIAL_PATH_ID_KEY, pathId);
        startActivity(launchTrailbookIntent);*/
        TrailBookState.setZoomToPathId(pathId);
        finish();
    }

    private void displayResults(ArrayList<KeyWord> mResults) {
        setContentView(R.layout.search_results_list);

        ListView listView = (ListView)findViewById(R.id.lv_results);
        mAdapter = new KeyWordArrayAdapter(this, mResults);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        //registerForContextMenu(listView);
    }

    private void addOnClickListener(ListView view)
    {
        view.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                //view.showContextMenu();
                KeyWord keyWord = (KeyWord)mAdapter.getItem(position);
                launchMapForPath(keyWord.pathId);
            }
        });
    }

    private ArrayList<KeyWord> getSearchResults(String search) {
        ArrayList<KeyWord> results = new ArrayList<KeyWord>();
        KeyWordDAO keyWordDAO = new KeyWordDAO(this);
        keyWordDAO.open();
        try {
            results = keyWordDAO.getKeyWordsMatching(search);
        }catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, CLASS_NAME + " exception getting suggestion", e);
        } finally {
            keyWordDAO.close();
        }
        return results;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " clicked " + position);
        KeyWord keyWord = (KeyWord)mAdapter.getItem(position);
        PathSummary summary = PathManager.getInstance().getPathSummary(keyWord.pathId);
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " need to launch main activity for action " + summary.getName());
        launchMapForPath(summary.getId());
    }

    private class KeyWordArrayAdapter extends BaseAdapter {
        private final Context context;
        private ArrayList<KeyWord> keyWords;

        public KeyWordArrayAdapter(Context context, ArrayList<KeyWord> keyWords) {
            this.context = context;
            this.keyWords = keyWords;
        }

        @Override
        public int getCount() {
            if (keyWords != null)
                return keyWords.size();
            else
                return 0;
        }

        @Override
        public Object getItem(int position) {
            if (keyWords != null)
                return keyWords.get(position);
            else
                return null;
        }

        @Override
        public long getItemId(int position) {
            if (keyWords == null)
                return 0;

            KeyWord item = keyWords.get(position);
            return item._id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            KeyWord keyWord = null;
            if (keyWords != null) {
                keyWord = keyWords.get(position);
            }
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.two_line_list_item, parent, false);
            TextView textView1 = (TextView) rowView.findViewById(R.id.text1);
            textView1.setText(ApplicationUtils.getLabelForKeywordType(keyWord.type) + keyWord.keyWord);
            TextView textView2 = (TextView) rowView.findViewById(R.id.text2);
            textView2.setText("  Path: " + keyWord.pathName);

            return rowView;
        }
    }
}