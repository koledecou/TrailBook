package com.trailbook.kole.fragments.list_content;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.PathSummary;

import java.util.ArrayList;

public class CheckablePathAdapter extends ArrayAdapter<PathSummary> {

    private ArrayList<PathSummary> mPathSummaries;
    private ArrayList<String> mPathSummariesToUpdate;

    public CheckablePathAdapter(Context context, int textViewResourceId,
                                ArrayList<PathSummary> pathSummaries) {
        super(context, textViewResourceId, pathSummaries);
        this.mPathSummaries = new ArrayList<PathSummary>();
        this.mPathSummaries.addAll(pathSummaries);
        mPathSummariesToUpdate = new ArrayList<String>();
        addAllToUpdateList(mPathSummaries);
    }

    private void addAllToUpdateList(ArrayList<PathSummary> summaries) {
        for (PathSummary s:summaries) {
            mPathSummariesToUpdate.add(s.getId());
        }
    }

    private class ViewHolder {
        CheckBox pathCheckBox;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater)this.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.checkbox_list_item, null);

            holder = new ViewHolder();
            holder.pathCheckBox = (CheckBox) convertView.findViewById(R.id.checkBox1);
            convertView.setTag(holder);

            holder.pathCheckBox.setOnClickListener( new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v ;
                    PathSummary summary = (PathSummary) cb.getTag();
                    if (cb.isChecked()) {
                        mPathSummariesToUpdate.add(summary.getId());
                    } else {
                        mPathSummariesToUpdate.remove(summary.getId());
                    }
                }
            });
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        PathSummary summary = mPathSummaries.get(position);
        holder.pathCheckBox.setText(summary.getName());
        holder.pathCheckBox.setChecked(true);
        holder.pathCheckBox.setTag(summary);

        return convertView;
    }

    public ArrayList<String> getPathSummariesToUpdate() {
        return mPathSummariesToUpdate;
    }
}