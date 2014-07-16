package com.trailbook.kole.services;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.events.NotesReceivedEvent;
import com.trailbook.kole.events.PathPointsReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;

/**
 * Created by Fistik on 7/2/2014.
 */
public  interface TrailbookPathServices {
    @GET(Constants.pathSummaryScript)
    void getPathSummaries(@QueryMap Map<String, String> options, Callback<ArrayList<PathSummary>> cb);

    @GET(Constants.getPathPointsScript)
    void getPathPoints(@QueryMap Map<String, String> options, Callback<PathPointsReceivedEvent.PathIDWithPoints> cb);

    @GET(Constants.getNotesScript)
    void getNotes(@QueryMap Map<String, String> options, Callback<NotesReceivedEvent.PathIDWithNotes> cb);
}
