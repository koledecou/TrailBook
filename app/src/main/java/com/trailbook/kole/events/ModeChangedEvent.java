package com.trailbook.kole.events;

/**
 * Created by kole on 9/16/2014.
 */
public class ModeChangedEvent {
    int newMode;
    int oldMode;

    public ModeChangedEvent(int oldMode, int newMode) {
        this.newMode = newMode;
        this.oldMode = oldMode;
    }

    public int getNewMode(){
        return newMode;
    }

    public int getOldMode(){
        return oldMode;
    }
}
