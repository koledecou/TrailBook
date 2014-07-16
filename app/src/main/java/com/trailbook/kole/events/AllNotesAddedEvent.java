package com.trailbook.kole.events;

import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PointAttachedObject;

import java.util.ArrayList;

/**
 * Created by Fistik on 7/14/2014.
 */
public class AllNotesAddedEvent {
    ArrayList<PointAttachedObject<Note>> paoNotes;
    String pathId;
    public AllNotesAddedEvent(ArrayList<PointAttachedObject<Note>> paoNotes, String pathId) {
        this.paoNotes = paoNotes;
        this.pathId = pathId;
    }

    public ArrayList<PointAttachedObject<Note>> getPaoNotes() {
        return paoNotes;
    }

    public String getPathId() {
        return pathId;
    }
}
