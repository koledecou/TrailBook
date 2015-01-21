package com.trailbook.kole.helpers;

import android.util.Log;

import com.trailbook.kole.data.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Unzipper
{
    List<String> fileList;
    private String mFileToUnzip = "C:\\MyFile.zip";
    private String mOutputFolder = "C:\\outputzip";

/*    public static void main( String[] args )
    {
        Unzipper unZip = new Unzipper();
        unZip.unZipIt(INPUT_ZIP_FILE,OUTPUT_FOLDER);
    }
    */
    public Unzipper(String fileToUnzip, String outputFolder) {
        mFileToUnzip = fileToUnzip;
        mOutputFolder = outputFolder;
    }

    public void unZipIt(){

        byte[] buffer = new byte[1024];

        try{

            //create output directory is not exists
            File folder = new File(mOutputFolder);
            if(!folder.exists()){
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis =
                    new ZipInputStream(new FileInputStream(mFileToUnzip));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while(ze!=null){

                String fileName = ze.getName();
                File newFile = new File(mOutputFolder + File.separator + fileName);

                Log.d(Constants.TRAILBOOK_TAG, "file unzip : " + newFile.getAbsoluteFile());

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

            Log.d(Constants.TRAILBOOK_TAG, "done unzipping");

        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
}