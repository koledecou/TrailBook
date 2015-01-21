package com.trailbook.kole.helpers;

import android.app.Fragment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Attachment;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.fragments.point_attached_object_create.CreateClimbFragment;
import com.trailbook.kole.fragments.point_attached_object_create.CreateNoteFragment;
import com.trailbook.kole.fragments.point_attched_object_view.PointAttachedObjectView;
import com.trailbook.kole.fragments.point_attched_object_view.climb.FullClimbView;
import com.trailbook.kole.fragments.point_attched_object_view.climb.SmallClimbView;
import com.trailbook.kole.fragments.point_attched_object_view.note.FullNoteView;
import com.trailbook.kole.fragments.point_attched_object_view.note.SmallNoteView;
import com.trailbook.kole.state_objects.TrailBookState;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

public class NoteFactory {
    public static final String NOTE = "Note";
    public static final String CLIMB = "Climb";
    public static final String COMMENT = "Comment";
    private static final BidiMap<String, String> classNameMap;
    private static final String DEFAULT_CLASS_NAME = "com.trailbook.kole.data.Note";
    private static final String DEFAULT_SHORT_NAME = "Note";

    static {
        Log.d(Constants.TRAILBOOK_TAG, "NoteFactory: initializing claaNameMap");
        classNameMap = new DualHashBidiMap<String, String>();
        classNameMap.put("Note", "com.trailbook.kole.data.Note");
        classNameMap.put("Climb", "com.trailbook.kole.data.Climb");
        classNameMap.put("Comment", "com.trailbook.kole.data.TrailBookComment");
    }

    public static String mapClassToShortName(String className) {
        if (className == null)
            return DEFAULT_SHORT_NAME;

        String shortName = classNameMap.getKey(className);
        if (shortName != null)
            return shortName;
        else
            return DEFAULT_SHORT_NAME;
    }

    public static String mapShortNameToClass(String shortName) {
        if (shortName == null)
            return DEFAULT_CLASS_NAME;

        String className = classNameMap.get(shortName);
        if (className != null)
            return className;
        else
            return DEFAULT_CLASS_NAME;
    }

    public static String getJsonFromPointAttachedObject(PointAttachedObject pao) {
        GsonBuilder gsonBilder = new GsonBuilder();
        gsonBilder.registerTypeAdapter(Attachment.class, new InterfaceAdapter<Attachment>());
        gsonBilder.setPrettyPrinting();
        Gson gson =gsonBilder.create();

        return gson.toJson(pao);
    }

    public static PointAttachedObject getPointAttachedObjectFromJSONString(String jsonString) {
        Log.d(Constants.TRAILBOOK_TAG, "NoteFactory: making pao from " + jsonString);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Attachment.class, new InterfaceAdapter<Attachment>());
        gsonBuilder.setPrettyPrinting();
        Gson gson =gsonBuilder.create();

        PointAttachedObject pao = gson.fromJson(jsonString,PointAttachedObject.class);
        Log.d(Constants.TRAILBOOK_TAG, "NoteFactory: pao is " + pao);
        return pao;
    }

    public static int getSelectedIconId(String attachmentType) {
        if (attachmentType.equals(NOTE)) {
            return R.drawable.info_bold;
        } else if (attachmentType.equals(CLIMB)) {
            return  R.drawable.climb_marker;
        } else {
            return R.drawable.ic_map_note_selected;
        }
    }

    public static int getUnelectedIconId(String attachmentType) {
        if (attachmentType.equals(NOTE)) {
            return R.drawable.info;
        } else if (attachmentType.equals(CLIMB)) {
            return  R.drawable.climb_marker;
        } else {
            return R.drawable.ic_map_note_unselected;
        }
    }

    public static PointAttachedObjectView getFullScreenView(PointAttachedObject paObject) {
        Log.d(Constants.TRAILBOOK_TAG, "NoteFactory: getting full screen view");
        if (paObject.getAttachment().getType().equals(NOTE)) {
            PointAttachedObjectView v = new FullNoteView(TrailBookState.getInstance());
            v.setPaoId(paObject.getId());
            return v;
        } else if (paObject.getAttachment().getType().equals(CLIMB)) {
            //todo: implement climb etc...
            PointAttachedObjectView v = new FullClimbView(TrailBookState.getInstance());
            v.setPaoId(paObject.getId());
            return v;
        } else {
            return null;
        }
    }

    public static PointAttachedObjectView getPaoSmallView(PointAttachedObject paObject) {
        if (paObject.getAttachment().getType().equals(NOTE)) {
            PointAttachedObjectView v = new SmallNoteView(TrailBookState.getInstance());
            v.setPaoId(paObject.getId());
            return v;
        } else if (paObject.getAttachment().getType().equals(CLIMB)) {
            //todo: implement climb etc...
            PointAttachedObjectView v = new SmallClimbView(TrailBookState.getInstance());
            v.setPaoId(paObject.getId());
            return v;
        } else {
            return null;
        }
    }

    public static Fragment getCreatePointObjectFragment(String paoId, String type) {
        if (CLIMB.equals(type)) {
            return CreateClimbFragment.newInstance(paoId);
        } else {
            return CreateNoteFragment.newInstance(paoId);
        }
    }
}
