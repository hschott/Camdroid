package org.camdroid;

import java.text.DecimalFormat;

import android.os.SystemClock;

public class FpsMeter {
	private static final DecimalFormat FPS_FORMAT = new DecimalFormat("0.00");

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
				this.framesPerSecond = FPS_FORMAT.format(this.fps) + " FPS@"
						+ Integer.valueOf(this.width) + "x"
						+ Integer.valueOf(this.height);
			} else {
				this.framesPerSecond = FPS_FORMAT.format(this.fps) + " FPS";
			}
		}

		StringBuilder sb = new StringBuilder(this.framesPerSecond);
		sb.append(' ');
		sb.append('|');
		for (int i = 0; i < this.frames; i++) {
			sb.append('.');
		}
		for (int i = 0; i < STEP - this.frames - 1; i++) {
			sb.append(' ');
		}
		sb.append('|');
		return sb.toString();

	}

}
