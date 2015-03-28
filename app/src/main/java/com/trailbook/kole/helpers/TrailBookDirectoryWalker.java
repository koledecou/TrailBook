package com.trailbook.kole.helpers;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class TrailBookDirectoryWalker extends DirectoryWalker {
    //TODO: should use filefilter.  bug in apache commons?
//    private static FileFilter tbFilter = FileFilterUtils.suffixFileFilter(".tb");
//    private static IOFileFilter tbFilter = FileFilterUtils.and(FileFilterUtils.fileFileFilter(), FileFilterUtils.suffixFileFilter("tb"));
    String suffix;
    public TrailBookDirectoryWalker(String suffix) {
        super();
        this.suffix = suffix;
        //super(tbFilter, -1);
    }

    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {
        if (isValidFile(file)) {
            String fileContents = FileUtils.readFileToString(file);
            results.add(fileContents);
        }
    }

    private boolean isValidFile(File file) {
        if (file.getName().endsWith(suffix))
            return true;
        else
            return false;
    }

    public ArrayList<String> getFileContentsFromDevice(String rootPathDirString) {
        ArrayList<String> paths = new ArrayList<String>();
        try {
            File rootPathDir = new File(rootPathDirString);
            walk(rootPathDir, paths);
        }
        catch (IOException e) {
        }

        return paths;
    }
}

