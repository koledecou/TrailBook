package com.trailbook.kole.helpers;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.trailbook.kole.data.Climb;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PointAttachedObject;

import java.lang.reflect.Type;

public class NoteFactory {
    public static PointAttachedObject getNoteFromJSONString(String jsonString) {
        Type paoNoteType = getAttachmentTypeFromJsonString(jsonString);

        Gson gson = new Gson();
        PointAttachedObject note = gson.fromJson(jsonString, paoNoteType);
        note.updateAttachmentType();

        return note;
    }

    private static Type getAttachmentTypeFromJsonString(String jsonString) {
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(jsonString);
        JsonElement element = (JsonElement)jsonObject.get("attachmentType");
        String attachmentType = "Note";
        if (element != null) {
            attachmentType = element.getAsString();
            Log.d(Constants.TRAILBOOK_TAG, "TrailbookPathUtilities: attachment type is " + attachmentType);
        }
        return getPAOAttachmentType(attachmentType);
    }

    private static Type getPAOAttachmentType(String attachmentType) {
        if (attachmentType == "Climb") {
            return new TypeToken<PointAttachedObject<Climb>>() {}.getType();
        } else {
            return new TypeToken<PointAttachedObject<Note>>() {}.getType();
        }
    }
}
