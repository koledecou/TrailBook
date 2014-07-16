package com.trailbook.kole.events;

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
