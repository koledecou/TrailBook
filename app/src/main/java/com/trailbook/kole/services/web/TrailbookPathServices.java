package com.trailbook.kole.services.web;

import com.trailbook.kole.data.Constants;

import retrofit.Callback;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.mime.TypedFile;

/**
 * Created by Fistik on 7/2/2014.
 */
public  interface TrailbookPathServices {
    @Multipart
    @POST(Constants.uploadImage)
    void uploadImage(
            @Part("pathid") String pathId,
            @Part("file_name") String fileName,
            @Part("file") TypedFile imageFile,
            Callback<String> cb);
}
