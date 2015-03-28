package com.trailbook.kole.helpers;

import org.apache.commons.io.DirectoryWalker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class PathExtractDirectoryWalker extends DirectoryWalker {
    private ArrayList<File> segmentDirectories;
    private ArrayList<File> noteFiles;
    private ArrayList<File> imageFiles;
    private File summaryFile;
    private String pathId;

    public PathExtractDirectoryWalker() {
        super();
        segmentDirectories = new ArrayList<File>();
        noteFiles = new ArrayList<File>();
        imageFiles = new ArrayList<File>();
    }

    public ArrayList<File> getSegmentDirectories() {
        return segmentDirectories;
    }

    public ArrayList<File> getNoteFiles() {
        return noteFiles;
    }

    public ArrayList<File> getImageFiles() {
        return imageFiles;
    }

    public File getSummaryFile() {
        return summaryFile;
    }

    public String getPathId() {
        return pathId;
    }

    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {
        String name = file.getName();
        String parent = file.getParentFile().getName();
        if (name != null && name.contains("_summary")) {
            pathId = getPathIdFromSummaryFile(name);
            summaryFile = file;
        } else if (parent != null && parent.contains("notes")) {
            noteFiles.add(file);
        } else if (parent != null && parent.contains("images")) {
            imageFiles.add(file);
        }
    }

    private String getPathIdFromSummaryFile(String summaryFileName) {
        String pathId = null;
        int index = summaryFileName.indexOf("_");
        if (index > 0)
            pathId = summaryFileName.substring(0, index);

        return pathId;
    }

    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
        String name = directory.getName();
        String parent = directory.getParentFile().getName();
        if (parent != null && parent.contains("segments")) {
            segmentDirectories.add(directory);
        }
        return super.handleDirectory(directory, depth, results);
    }

    public void putPathFilesInFolders(String rootPathDirString) {
        ArrayList<String> results = new ArrayList<String>();
        try {
            File rootPathDir = new File(rootPathDirString);
            walk(rootPathDir, results);
        }
        catch (IOException e) {
        }
    }
}

