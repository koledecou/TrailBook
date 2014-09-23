package com.trailbook.kole.events;

import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PointAttachedObject;

/**
 * Created by Fistik on 7/12/2014.
 */
public class NoteAddedEvent {

    PointAttachedObject<Note> paoNote;
    public NoteAddedEvent(PointAttachedObject<Note> paoNote) {
        this.paoNote = paoNote;
    }

    public PointAttachedObject<Note> getPaoNote() {
        return paoNote;
    }
}

