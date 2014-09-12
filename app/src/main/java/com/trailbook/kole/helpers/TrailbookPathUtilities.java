package com.trailbook.kole.helpers;

import android.content.res.Resources;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathContainer;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.state_objects.PathManager;

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
    private static String newNoteId;

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

    public static double getDistanceToNote(PointAttachedObject<Note> paoNote, Location l){
        LatLng noteLoc = paoNote.getLocation();
        Note note = paoNote.getAttachment();
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


    public static String getNewPathId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }

    public static String getNewSegmentId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }

    public static String getPathSummaryJSONString(PathSummary summary) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        if (gson == null) {
            Log.d(Constants.TRAILBOOK_TAG, "error creating gson");
        }
        return gson.toJson(summary);
    }

    public static String getSegmentListJSONString(Path path) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(path.getSegmentIdList());
    }

    public static String getSegmentNotesJSONString(PathSegment segment) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(segment.getPointNotes());
    }

    public static String getSegmentPointsJSONString(PathSegment segment) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(segment.getPoints());
    }

    public static String getPathJSONString(Path path) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(path);
    }

    public static String getSegmentJSONString(PathSegment s) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(s);
    }

    public static String getPathSegmentMapJSONString(Path p) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(p.getSegmentIdList());
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

    public static PathContainer parseXML(String pathXML) {
        Log.d("path xml", pathXML);
        Document pathXMLDoc = TrailbookPathUtilities.XMLfromString(pathXML);
        pathXMLDoc.getDocumentElement ().normalize ();

        Path p = new Path(getNewPathId());
        PathSegment s = new PathSegment((getNewSegmentId()));
        ArrayList<PathSegment> segments = new ArrayList<PathSegment>();
        ArrayList<PointAttachedObject<Note>> paoNotes = new ArrayList<PointAttachedObject<Note>>();

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
                    PathSummary summary = p.getSummary();
                    summary.setName(pathName);
                    if (points.size()>0) {
                        summary.setStart(points.get(0));
                        summary.setEnd(points.get(points.size() - 1));
                    }
                } else if (getPlaceMarkType(placeMarkElement) == PLACEMARK_TYPE_NOTE) {
                    NodeList pointList = placeMarkElement.getElementsByTagName("Point");
                    Element pointElement = (Element) pointList.item(0);
                    NodeList coordsNodeList = pointElement.getElementsByTagName("coordinates");
                    NodeList markNameList = placeMarkElement.getElementsByTagName("description");

                    LatLng point = getLatLngFromCoordsList(coordsNodeList);
                    Note newNote = new Note(getNewNoteId(), s.getId());
                    if (point != null) {
                        PointAttachedObject<Note> paoNote = new PointAttachedObject<Note>(point, newNote);
                        String noteContent = getNoteContentFromPlaceMarkNameList(markNameList);
                        paoNote.getAttachment().setNoteContent(noteContent);
                        s.addPointNote(paoNote);
                    }
                }
            }
        }
        p.addSegment(s.getId());
        segments.add(s);

        return new PathContainer(p, segments);
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
            Log.e(Constants.TRAILBOOK_TAG, "Error parsing kml: ", e);
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
}
