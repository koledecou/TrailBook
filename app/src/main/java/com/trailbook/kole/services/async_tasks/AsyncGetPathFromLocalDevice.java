package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;

import com.trailbook.kole.data.Path;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncGetPathFromLocalDevice extends AsyncTask<String, Void, ArrayList<Path>> {

    @Override
    protected ArrayList<Path> doInBackground(String... pathIds) {
        PathManager manager = PathManager.getInstance();
        ArrayList<Path> paths = new ArrayList<Path>();
        try {
            for (String pathId:pathIds) {
                if (!manager.isStoredLocally(pathId))
                    continue;
                Path path = manager.loadPathFromDevice(pathId);
                if (path != null && path.summary != null) {
                    paths.add(path);
                }
            }
        } catch (Exception e) {
        }
        return paths;
    }

    @Override
    protected void onPostExecute(ArrayList<Path> paths) {
        if (paths == null)
            return;
        for (Path path:paths) {
            ApplicationUtils.postPathEvents(path);
        }

        super.onPostExecute(paths);
    }
}
