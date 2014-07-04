package com.trailbook.kole.services;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;

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
public  interface PathService {
    @GET(Constants.pathSummaryScript)
    void getPaths(@QueryMap Map<String, String> options, Callback<ArrayList<PathSummary>> cb);
}
