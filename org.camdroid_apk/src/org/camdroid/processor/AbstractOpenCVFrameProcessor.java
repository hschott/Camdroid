package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.graphics.Bitmap;

public abstract class AbstractOpenCVFrameProcessor implements FrameProcessor {

	protected volatile boolean locked = false;
	protected Mat in;
	protected FrameDrawer drawer;
	protected Bitmap out;
	protected int width = 0;
	protected int height = 0;

	static {
		OpenCVLoader.init();
	}

	public AbstractOpenCVFrameProcessor(FrameDrawer drawer) {
		this.drawer = drawer;
	}

	public boolean isLocked() {
		return locked;
	}

	@Override
	public boolean put(byte[] data) {
		if (locked || in == null)
			return false;
		locked = true;
		this.in.put(0, 0, data);
		locked = false;
		return true;
	}

	@Override
	public void allocate(int width, int height) {
		this.width = width;
		this.height = height;
		in = new Mat(this.height + (this.height / 2), this.width,
				CvType.CV_8UC1);
		out = Bitmap.createBitmap(this.width, this.height,
				Bitmap.Config.ARGB_8888);
	}

	@Override
	public void release() {
		if (in != null) {
			in.release();
		}
		if (out != null) {
			out.recycle();
		}
	}
}