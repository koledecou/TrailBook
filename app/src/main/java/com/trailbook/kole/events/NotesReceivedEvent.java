package com.trailbook.kole.events;

import android.graphics.Point;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Fistik on 7/5/2014.
 */
public class NotesReceivedEvent {
    public class SegmentIDWithNotes {
        String segmentId;
        HashMap<String,PointAttachedObject<Note>> pointNotes;

        public SegmentIDWithNotes (String segmentId, HashMap<String,PointAttachedObject<Note>> pointNotes) {
            this.segmentId = segmentId;
            this.pointNotes = pointNotes;
        }

        public HashMap<String,PointAttachedObject<Note>> getPointNotes() {
            return pointNotes;
        }
        public String getSegmentId() {
            return segmentId;
        }
    }

    SegmentIDWithNotes result;

    public NotesReceivedEvent(SegmentIDWithNotes segmentIDWithNotes) {
        result = segmentIDWithNotes;
    }
    public NotesReceivedEvent(String segmentId, HashMap<String,PointAttachedObject<Note>> notes) {
        result = new SegmentIDWithNotes(segmentId, notes);
    }

    public HashMap<String,PointAttachedObject<Note>> getPointNotes() {
        return result.getPointNotes();
    }
    public String getSegmentId() {
        return result.getSegmentId();
    }
}
