package com.bmh.ms101.PhotoFlipping;

import java.io.File;

/**
 * Created by Main on 11/5/14.
 */
public class ImageFilePath {

    private File myFile;
    private String myPath;

    public ImageFilePath(File file, String path) {
        myFile = file;
        myPath = path;
    }

    public File getFile() {
        return myFile;
    }

    public String getPath() {
        return myPath;
    }
}
