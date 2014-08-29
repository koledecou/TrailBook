package com.trailbook.kole.services;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.events.NotesReceivedEvent;
import com.trailbook.kole.events.PathSegmentMapRecievedEvent;
import com.trailbook.kole.events.SegmentPointsReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import retrofit.mime.TypedFile;

/**
 * Created by Fistik on 7/2/2014.
 */
public  interface TrailbookPathServices {
    @GET(Constants.pathSummaryScript)
    void getPathSummaries(@QueryMap Map<String, String> options, Callback<ArrayList<PathSummary>> cb);

    @GET(Constants.getSegmentsScript)
    void getPathSegmentMap(@QueryMap Map<String, String> options, Callback<PathSegmentMapRecievedEvent.SegmentListWithPathID> cb);

    @GET(Constants.getPointsScript)
    void getPoints(@QueryMap Map<String, String> options, Callback<SegmentPointsReceivedEvent.SegmentIDWithPoints> cb);

    @GET(Constants.getNotesScript)
    void getNotes(@QueryMap Map<String, String> options, Callback<NotesReceivedEvent.SegmentIDWithNotes> cb);

    @FormUrlEncoded
    @POST(Constants.uploadJson)
    void postStringFileContents(@Field("contents") String fileContents, @Field("dir") String dir, @Field("fileName") String fileName, Callback<String> cb);

    @Multipart
    @POST(Constants.uploadImage)
    void uploadImage(
            @Part("pathid") String pathId,
            @Part("file_name") String fileName,
            @Part("file") TypedFile imageFile,
            Callback<String> cb);
}
