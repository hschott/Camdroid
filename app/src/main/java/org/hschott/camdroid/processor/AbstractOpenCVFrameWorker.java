package org.hschott.camdroid.processor;

import android.graphics.Bitmap;

import org.hschott.camdroid.OnCameraPreviewListener;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractOpenCVFrameWorker implements FrameWorker {

    protected final ReentrantLock lock = new ReentrantLock();
    protected Mat in;
    protected Mat out;
    private Mat rgb;
    private Mat hsv;
    protected OnCameraPreviewListener.FrameDrawer drawer;
    protected Bitmap bmp;
    protected int width = 0;
    protected int height = 0;
    private boolean processed = true;

    static {
        OpenCVLoader.initDebug();
    }

    public AbstractOpenCVFrameWorker(OnCameraPreviewListener.FrameDrawer drawer) {
        this.drawer = drawer;
    }

    @Override
    public void allocate(int width, int height) {
        this.width = width;
        this.height = height;
        this.in = new Mat(this.height + (this.height / 2), this.width,
                CvType.CV_8UC1);
        this.out = new Mat();
        this.rgb = new Mat();//new Mat(this.height, this.width, CvType.CV_8UC3);
        this.hsv = new Mat();//new Mat(this.height, this.width, CvType.CV_8UC3);
        this.bmp = Bitmap.createBitmap(this.width, this.height,
                Bitmap.Config.ARGB_8888);
    }

    @Override
    public boolean put(byte[] data) {
        if (this.lock.isLocked()) {
            return false;
        }

        if (this.in == null) {
            return false;
        }

        this.lock.lock();
        try {
            if (processed) {
                this.in.put(0, 0, data);
                processed = false;
            }
        } finally {
            this.lock.unlock();
        }

        return true;
    }

    @Override
    public void release() {
        this.lock.lock();
        try {
            if (this.in != null) {
                this.in.release();
            }
            if (this.out != null) {
                this.out.release();
            }
            if (this.rgb != null) {
                this.rgb.release();
            }
            if (this.hsv != null) {
                this.hsv.release();
            }
            if (this.bmp != null) {
                this.bmp.recycle();
            }
        } finally {
            this.lock.unlock();
        }
    }


    @Override
    public void run() {
        if (this.lock.isLocked()) {
            return;
        }

        if (this.in == null || this.bmp == null) {
            return;
        }

        this.lock.lock();


        try {
            if (!processed) {
                execute();

                draw();

                processed = true;
            }
        } finally {
            this.lock.unlock();
        }
    }

    protected void draw() {
        Utils.matToBitmap(out, this.bmp);
        this.drawer.drawBitmap(this.bmp);
    }

    protected abstract void execute();

    protected Mat rgb() {
        Imgproc.cvtColor(this.in, this.rgb, Imgproc.COLOR_YUV420sp2RGB);
        return rgb;
    }

    protected Mat gray() {
        return this.in.submat(0, this.height, 0, this.width);
    }

    protected Mat hsv() {
        Imgproc.cvtColor(rgb(), hsv, Imgproc.COLOR_RGB2HSV);
        return hsv;
    }

    protected Mat split(Mat mat, int c) {
        List<Mat> channels = new ArrayList<Mat>();
        Core.split(mat, channels);
        return channels.get(c);
    }
}
