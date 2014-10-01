package com.trailbook.kole.events;

/**
 * Created by Fistik on 7/3/2014.
 */
public class PointAttachedObjectDeletedEvent {

    String paoId;
    public PointAttachedObjectDeletedEvent(String paoId) {
        this.paoId=paoId;
    }

    public String getPaoId() {
        return paoId;
    }
}
