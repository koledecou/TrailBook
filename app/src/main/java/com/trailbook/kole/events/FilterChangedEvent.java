package com.trailbook.kole.events;

import com.trailbook.kole.data.FilterSet;

/**
 * Created by kole on 12/17/2014.
 */
public class FilterChangedEvent {
    private FilterSet mFilters;
    public FilterChangedEvent(FilterSet filters) {
        this.mFilters = filters;
    }

    public FilterSet getFilters() {
        return mFilters;
    }
}
