package org.opencv.android;


/**
 * Helper class provides common initialization methods for OpenCV library.
 */
public class OpenCVLoader
{
    /**
     * OpenCV Library version 2.4.2.
     */
    public static final String OPENCV_VERSION_2_4_2 = "2.4.2";

    /**
     * OpenCV Library version 2.4.3.
     */
    public static final String OPENCV_VERSION_2_4_3 = "2.4.3";

    /**
     * OpenCV Library version 2.4.4.
     */
    public static final String OPENCV_VERSION_2_4_4 = "2.4.4";

    /**
     * OpenCV Library version 2.4.5.
     */
    public static final String OPENCV_VERSION_2_4_5 = "2.4.5";


    /**
     * Loads and initializes OpenCV library from current application package. Roughly, it's an analog of system.loadLibrary("opencv_java").
     * @return Returns true is initialization of OpenCV was successful.
     */
    public static boolean init()
    {
        return StaticHelper.initOpenCV();
    }

}
