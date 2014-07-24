package com.trailbook.kole.events;

import android.graphics.Point;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;

import java.util.ArrayList;

/**
 * Created by Fistik on 7/5/2014.
 */
public class NotesReceivedEvent {
    public class PathIDWithNotes {
        int nImages = 0;
        String pathId;
        ArrayList<PointAttachedObject<Note>> pointNotes;
        ArrayList<Note> pathNotes;

        public PathIDWithNotes (String pathId, ArrayList<PointAttachedObject<Note>> pointNotes) {
            this.pathId = pathId;
            this.pointNotes = pointNotes;
        }

        public PathIDWithNotes (String pathId, PointAttachedObject<Note> pointNote) {
            this.pathId = pathId;
            ArrayList<PointAttachedObject<Note>> noteArray = new ArrayList<PointAttachedObject<Note>>();
            noteArray.add(pointNote);
            this.pointNotes = noteArray;
        }

        public ArrayList<PointAttachedObject<Note>> getPointNotes() {
            return pointNotes;
        }
        public ArrayList<Note> getPathNotes() {
            return pathNotes;
        }

        public String getPathId() {
            return pathId;
        }
    }

    PathIDWithNotes result;

    public NotesReceivedEvent(PathIDWithNotes pathIdWithNotes) {
        result = pathIdWithNotes;
    }
    public NotesReceivedEvent(String pathId, ArrayList<PointAttachedObject<Note>> notes) {
        result = new PathIDWithNotes(pathId, notes);
    }
    public NotesReceivedEvent(String pathId, PointAttachedObject<Note> note) {
        result = new PathIDWithNotes(pathId, note);
    }

    public ArrayList<PointAttachedObject<Note>> getPointNotes() {
        return result.getPointNotes();
    }
    public ArrayList<Note> getPathNotes() {
        return result.getPathNotes();
    }
    public String getPathId() {
        return result.getPathId();
    }
}
