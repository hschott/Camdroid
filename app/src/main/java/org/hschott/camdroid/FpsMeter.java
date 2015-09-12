package org.hschott.camdroid;

import android.os.SystemClock;

import java.text.DecimalFormat;

public class FpsMeter {
    private static final DecimalFormat FPS_FORMAT = new DecimalFormat("0.00");
    private static final String X = "x";
    private static final String FPS = " FPS";
    private static final String AT = "@";

    private long prevTs;
    private String framesPerSecond;
    private boolean isInitialized = false;
    private int width = 0;
    private int height = 0;

    private long frames;
    private double fps;
    private static final int STEP = 10;

    public FpsMeter(int width, int height) {
        super();
        this.width = width;
        this.height = height;
    }

    private void _measure() {
        if (!this.isInitialized) {
            this.init();
        }
        this.frames++;
    }

    public double getFps() {
        return this.fps;
    }

    public void init() {
        this.frames = 0;
        this.prevTs = SystemClock.elapsedRealtime();
        this.framesPerSecond = "";
        this.isInitialized = true;
    }

    public String measure() {
        this._measure();
        if (this.frames % STEP == 0) {
            long ts = SystemClock.elapsedRealtime();
            this.fps = this.frames / ((double) (ts - this.prevTs) / 1000);
            this.prevTs = ts;
            this.frames = 0;
            if (this.width != 0 && this.height != 0) {
                this.framesPerSecond = FPS_FORMAT.format(this.fps) + FPS + AT
                        + this.width + X
                        + this.height;
            } else {
                this.framesPerSecond = FPS_FORMAT.format(this.fps) + FPS;
            }
        }

        return this.framesPerSecond;
    }

}
