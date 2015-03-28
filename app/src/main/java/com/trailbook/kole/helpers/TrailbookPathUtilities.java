package com.trailbook.kole.helpers;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.FilterSet;
import com.trailbook.kole.data.KeyWordHashCollection;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathGroup;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.data.User;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by kole on 7/19/2014.
 */
public class TrailbookPathUtilities {
    private static final int PLACEMARK_TYPE_LINE = 1;
    private static final int PLACEMARK_TYPE_NOTE = 2;

    public static LatLng getNearestPointOnPath(LatLng referenceLocation, String pathId) {
        double minDist = Double.MAX_VALUE;
        LatLng nearestPointOnPath = null;
        ArrayList<PathSegment> segments = PathManager.getInstance().getSegmentsForPath(pathId);
        if (segments == null || segments.size()<1 )
            return null;

        for (PathSegment s : segments) {
            LatLng nearestPointOnSegment = getNearestPointOnSegment(referenceLocation, s);
            double thisDist = getDistanceInMeters(nearestPointOnSegment, referenceLocation);
            if (thisDist < minDist) {
                minDist = thisDist;
                nearestPointOnPath = nearestPointOnSegment;
            }
        }

        return nearestPointOnPath;
    }

    public static void deletePointFromPath(String pathId, LatLng point) {
        ArrayList<PathSegment> segments = PathManager.getInstance().getSegmentsForPath(pathId);
        if (segments == null || segments.size()<1 )
            return;

        for (PathSegment s : segments) {
            s.deletePoint(point);
        }
    }

    public static void movePoint(String pathId, LatLng oldLoc, LatLng newLoc) {
        ArrayList<PathSegment> segments = PathManager.getInstance().getSegmentsForPath(pathId);
        if (segments == null || segments.size()<1 )
            return;

        for (PathSegment s : segments) {
            s.movePoint(oldLoc, newLoc);
        }
    }

    public static void moveNote(String pathId, String paoId, LatLng newLoc) {
        PointAttachedObject pao = PathManager.getInstance().getPointAttachedObject(paoId);
        pao.move(newLoc);

    }

    private static LatLng getNearestPointOnSegment(LatLng referenceLocation, PathSegment s) {
        double minDist = Double.MAX_VALUE;
        LatLng closestPoint = null;
        for (LatLng thisPathPoint : s.getPoints()) {
            float thisDist = getDistanceInMeters(referenceLocation, thisPathPoint);
            if (thisDist < minDist) {
                minDist = thisDist;
                closestPoint = thisPathPoint;
            }
        }
        return closestPoint;
    }

    public static double getNearestDistanceFromPointToPath(LatLng currentLocation, String pathId) {
        double minDist = Double.MAX_VALUE;
        ArrayList<PathSegment> segments = PathManager.getInstance().getSegmentsForPath(pathId);
        if (segments == null || segments.size()<1 )
            return -1d; //todo: should throw an exception here

        for (PathSegment s : segments) {
            double thisDist = getNearestDistanceFromSegment(currentLocation, minDist, s);
            if (thisDist < minDist)
                minDist = thisDist;
        }

        return minDist;
    }

    private static double getNearestDistanceFromSegment(LatLng currentLocation, double minDist, PathSegment s) {
        for (LatLng thisPathPoint : s.getPoints()) {
            float thisDist = getDistanceInMeters(currentLocation, thisPathPoint);
            if (thisDist < minDist)
                minDist = thisDist;
        }
        return minDist;
    }

    public static float getDistanceInMeters(LatLng p1, LatLng p2) {
        float[] results = new float[5];
        Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results);
        return results[0];
    }

    public static double getDistanceToNote(PointAttachedObject paObject, Location l){
        LatLng noteLoc = paObject.getLocation();
        double distanceToNote = TrailbookPathUtilities.getDistanceInMeters(TrailbookPathUtilities.locationToLatLon(l), noteLoc);
        return distanceToNote;
    }


    public static LatLng locationToLatLon(Location l) {
        return new LatLng(l.getLatitude(), l.getLongitude());
    }

    public static String getNewNoteId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }

    public static String getNewCommentId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }

    public static String getNewPathId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }

    public static String getNewSegmentId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }

    public static String getCommentJsonString(TrailBookComment comment) {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(comment);
    }

    public static String getPathSummaryJSONString(PathSummary summary) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(summary);
    }

    public static String getKeyHashJSON(KeyWordHashCollection hash) {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(hash);
    }

    public static String getSegmentPointsJSONString(PathSegment segment) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(segment.getPoints());
    }

    public static String getSegmentJSONString(PathSegment s) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(s);
    }

    public static String getQuickNoteContent(Resources r, int resourceId) {
        if (resourceId == R.id.quick_note_left)
            return r.getString(R.string.qn_left);
        else if (resourceId == R.id.quick_note_right)
            return r.getString(R.string.qn_right);
        else if (resourceId == R.id.quick_note_straight)
            return r.getString(R.string.qn_straight);
        else
            return null;
    }

    private static int getPlaceMarkType(Element placeMarkElement) {
        NodeList lineStringList = placeMarkElement.getElementsByTagName("LineString");
        if (lineStringList.getLength() > 0)
            return PLACEMARK_TYPE_LINE;
        else
            return PLACEMARK_TYPE_NOTE;
    }

    public static Path parseXML(String pathXML) {
        Document pathXMLDoc = TrailbookPathUtilities.XMLfromString(pathXML);
        pathXMLDoc.getDocumentElement ().normalize ();

        PathSummary summary = new PathSummary(getNewPathId());
        PathSegment s = new PathSegment((getNewSegmentId()));
        ArrayList<PathSegment> segments = new ArrayList<PathSegment>();
        ArrayList<PointAttachedObject> paoNotes = new ArrayList<PointAttachedObject>();

        NodeList listOfPlaceMarks = pathXMLDoc.getElementsByTagName("Placemark");
        for (int i = 0; i < listOfPlaceMarks.getLength(); i++) {
            Node placeMarkNode = listOfPlaceMarks.item(i);
            if (placeMarkNode.getNodeType() == Node.ELEMENT_NODE) {
                Element placeMarkElement = (Element) placeMarkNode;

                //if line string exists, then this placemark is the path coords.
                //the path name is in the name item, the coords is in coordinates.
                if (getPlaceMarkType(placeMarkElement) == PLACEMARK_TYPE_LINE) {
                    NodeList lineStringList = placeMarkElement.getElementsByTagName("LineString");
                    NodeList markNameList = placeMarkElement.getElementsByTagName("name");
                    ArrayList<LatLng> points = getLatLngArrayFromLineStringList(lineStringList);
                    s.addPoints(points);
                    String pathName = getNameFromPlaceMarkNameList(markNameList);
                    summary.setName(pathName);
                    if (points.size()>0) {
                        summary.setStart(points.get(0));
                        summary.setEnd(points.get(points.size() - 1));
                    }
                } else if (getPlaceMarkType(placeMarkElement) == PLACEMARK_TYPE_NOTE) {
                    NodeList pointList = placeMarkElement.getElementsByTagName("Point");
                    Element pointElement = (Element) pointList.item(0);
                    NodeList coordsNodeList = pointElement.getElementsByTagName("coordinates");
                    NodeList markNameList = placeMarkElement.getElementsByTagName("name");

                    LatLng point = getLatLngFromCoordsList(coordsNodeList);
                    Note newNote = new Note();
                    if (point != null) {
                        String noteId=TrailbookPathUtilities.getNewNoteId();
                        String noteContent = null;
                        PointAttachedObject paoNote = null;
                        try {
                            noteContent = getNoteContentFromPlaceMarkNameList(markNameList);
                            newNote.setNoteContent(noteContent);
                            paoNote = new PointAttachedObject(noteId, point, newNote);
                            paoNotes.add(paoNote);
                            summary.addPao(noteId);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        summary.addSegment(s.getId());
        segments.add(s);

        return new Path(summary, segments, paoNotes);
    }

    private static String getNoteContentFromPlaceMarkNameList(NodeList markNameList) {
        String name = "";
        Node placeMarkNameNode = (Node)markNameList.item(0);
        try {
            name = placeMarkNameNode.getFirstChild().getNodeValue();
        } catch (Exception e) {
            name = "";
        }
        return name;
    }

    private static LatLng getLatLngFromCoordsList(NodeList coordsNodeList) {
        Node coordNode = coordsNodeList.item(0);
        String coords = "";
        try {
            coords = coordNode.getFirstChild().getNodeValue();
        } catch (Exception e) {
            coords = "";
        }
        return parseCoordinate(coords);
    }

    private static String getNameFromPlaceMarkNameList(NodeList markNameList) {
        String name = "";
        Node placeMarkNameNode = (Node)markNameList.item(0);
        try {
            name = placeMarkNameNode.getFirstChild().getNodeValue();
        } catch (Exception e) {
            name = "";
        }
        return name;
    }

    private static ArrayList<LatLng> getLatLngArrayFromLineStringList(NodeList lineStringList) {
        Element lineStringElement = (Element)lineStringList.item(0);
        NodeList coordsNodeList = lineStringElement.getElementsByTagName("coordinates");
        Node coordNode = coordsNodeList.item(0);
        String coordinateString = "";
        try {
            coordinateString = coordNode.getFirstChild().getNodeValue();
        } catch (Exception e) {
            coordinateString = "";
        }
        ArrayList<LatLng> points = parsePathCoords(coordinateString);
        return points;
    }

    public static LatLng parseCoordinate(String coordString) {
        Pattern p = Pattern.compile(",");
        String[] sPoint = p.split(coordString);
        if (sPoint.length < 2)//not real result
            return null;
        try {
            LatLng point = new LatLng(Double.parseDouble(sPoint[1]), Double.parseDouble(sPoint[0]));
            return point;
        }catch (Exception e) {
            return null;
        }
    }

    public static ArrayList<LatLng> parsePathCoords(String coordString) {
        ArrayList<LatLng> coords = new ArrayList<LatLng>();
        Pattern p = Pattern.compile (" ");
        String[] rows = p.split(coordString);
        for (int i=0;i<rows.length;i++) {
            LatLng point = parseCoordinate(rows[i]);
            if (point != null)
                coords.add(point);
         }
        return coords;
    }

    public static Document XMLfromString(String xml){
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {

            DocumentBuilder db = dbf.newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is);

        } catch (ParserConfigurationException e) {
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return doc;
    }

    public static boolean hasEditPermissions(String pathId) {
        PathSummary summary = PathManager.getInstance().getPathSummary(pathId);
        if (summary ==  null)
            return false;

        if (TrailBookState.getCurrentUser() == null || TrailBookState.getCurrentUser().userId == null || TrailBookState.getCurrentUser().userId.length() < 1)
            return false;

        if (summary.getOwnerId() == null
             || summary.getOwnerId().length() < 1
             || summary.getOwnerId().equals(TrailBookState.getCurrentUser().userId)) {
            return true;
        } else {
            return false;
        }
    }

    public static ArrayList<PathGroup> getMyGroups() {
        return null;
    }

    public static FilterSet getFilters() {
        FilterSet filters = new FilterSet();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(TrailBookState.getInstance());
        filters.showMyPaths =  preferences.getBoolean(Constants.FILTER_PREFS_SHOW_MY_PATHS_KEY, true);
        filters.showOtherPaths =  preferences.getBoolean(Constants.FILTER_PREFS_SHOW_OTHER_PATHS_KEY, true);
        filters.groupIdsToShow = preferences.getStringSet(Constants.FILTER_PREFS_GROUPS_TO_SHOW_KEY, new LinkedHashSet<String>());
        return filters;
    }

    public static void saveFilters(FilterSet filters) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(TrailBookState.getInstance());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.FILTER_PREFS_SHOW_MY_PATHS_KEY, filters.showMyPaths);
        editor.putBoolean(Constants.FILTER_PREFS_SHOW_OTHER_PATHS_KEY, filters.showOtherPaths);
        editor.putStringSet(Constants.FILTER_PREFS_GROUPS_TO_SHOW_KEY, filters.groupIdsToShow);
        editor.commit();
    }

    public static boolean isPathInFilter(PathSummary pathSummary, FilterSet filters) {
        return true;
    }

/*    private static boolean isPathInGroups(PathSummary pathSummary, Set<String> groupIdsToShow) {
        for (String groupId:groupIdsToShow) {
            PathGroup group = PathManager.getInstance().getGroup(groupId);
            if (group.pathIds != null && group.pathIds.contains(groupId))
                return true;
        }

        return false;
    }*/

    private static boolean isPathMine(PathSummary pathSummary) {
        String ownerId = pathSummary.getOwnerId();
        User currentUser = TrailBookState.getCurrentUser();
        if (currentUser != null && currentUser.userId.equals(ownerId))
            return true;
        else
            return false;
    }

}
