package com.trailbook.kole.events;

import com.trailbook.kole.data.PointAttachedObject;

/**
 * Created by Fistik on 7/12/2014.
 */
public class MapObjectAddedEvent {

    PointAttachedObject pao;
    public MapObjectAddedEvent(PointAttachedObject pao) {
        this.pao = pao;
    }

    public PointAttachedObject getPao() {
        return pao;
    }
}

