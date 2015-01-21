package com.trailbook.kole.helpers;

import android.util.Log;

import com.trailbook.kole.data.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper
{
    List<String> fileList;
    private String mSourceFolder;
    private String mDestinationFileName;

    Zipper(String sourceFolder, String destinationFileName){
        fileList = new ArrayList<String>();
        mSourceFolder = sourceFolder;
        mDestinationFileName = destinationFileName;
    }

    public void zipIt(){
        generateFileList();

        byte[] buffer = new byte[1024];

        try{

            FileOutputStream fos = new FileOutputStream(mDestinationFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);

            Log.d(Constants.TRAILBOOK_TAG, "Output to Zip : " + mDestinationFileName);

            for(String file : this.fileList){
                Log.d(Constants.TRAILBOOK_TAG, "File Added : " + file);
                ZipEntry ze= new ZipEntry(file);
                zos.putNextEntry(ze);

                FileInputStream in =
                        new FileInputStream(mSourceFolder + File.separator + file);

                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                in.close();
            }

            zos.closeEntry();
            //remember close it
            zos.close();

            Log.d(Constants.TRAILBOOK_TAG, "Done.");
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public void generateFileList(){
        generateFileList(new File(mSourceFolder));
    }

    /**
     * Traverse a directory and get all files,
     * and add the file into fileList
     * @param node file or directory
     */
    public void generateFileList(File node){

        //add file only
        if(node.isFile()){
            fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
        }

        if(node.isDirectory()){
            String[] subNote = node.list();
            for(String filename : subNote){
                generateFileList(new File(node, filename));
            }
        }

    }

    /**
     * Format the file path for zip
     * @param file file path
     * @return Formatted file path
     */
    private String generateZipEntry(String file){
        return file.substring(mSourceFolder.length()+1, file.length());
    }
}