package com.trailbook.kole.tools;

import android.util.Log;

import com.trailbook.kole.data.Constants;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Fistik on 7/16/2014.
 */
public class PathDirectoryWalker extends DirectoryWalker {
    //TODO: should use filefilter.  bug in apache commons?
//    private static FileFilter tbFilter = FileFilterUtils.suffixFileFilter(".tb");
//    private static IOFileFilter tbFilter = FileFilterUtils.and(FileFilterUtils.fileFileFilter(), FileFilterUtils.suffixFileFilter("tb"));
    String suffix;
    public PathDirectoryWalker(String suffix) {
        super();
        this.suffix = suffix;
        //super(tbFilter, -1);
    }

    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {
        if (isPathFile(file)) {
            String fileContents = FileUtils.readFileToString(file);
            results.add(fileContents);
        }
    }

    private boolean isPathFile(File file) {
        Log.d(Constants.TRAILBOOK_TAG, "file = " + file);
        if (file.getName().endsWith(suffix))
            return true;
        else
            return false;
    }

    public ArrayList<String> getPathFileContentsFromDevice(String rootPathDirString) {
        ArrayList<String> paths = new ArrayList<String>();
        try {
            File rootPathDir = new File(rootPathDirString);
            walk(rootPathDir, paths);
        }
        catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Problem finding paths!", e);
        }

        return paths;
    }
}

