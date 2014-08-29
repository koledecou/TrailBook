package com.trailbook.kole.events;
import java.util.ArrayList;

/**
 * Created by kole on 8/20/2014.
 */
public class PathSegmentMapRecievedEvent {
    public class SegmentListWithPathID {
        String pathId;
        ArrayList<String> segmentIds;

        public ArrayList<String> getSegmentIds() {
            return segmentIds;
        }
        public String getPathId() {
            return pathId;
        }
    }

    SegmentListWithPathID result;
    public PathSegmentMapRecievedEvent(SegmentListWithPathID segmentListWithPathID) {
        result = segmentListWithPathID;
    }

    public ArrayList<String> getSegmentIds() {
        return result.getSegmentIds();
    }
    public String getPathId() {
        return result.getPathId();
    }
}
