package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;

public abstract class AbstractOpenCVFrameProcessor implements FrameProcessor {

	protected volatile boolean locked = false;
	protected Mat in;
	private Mat rgb;
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

	@Override
	public void allocate(int width, int height) {
		this.width = width;
		this.height = height;
		this.in = new Mat(this.height + (this.height / 2), this.width,
				CvType.CV_8UC1);
		this.rgb = new Mat(this.height, this.width, CvType.CV_8UC3);
		this.out = Bitmap.createBitmap(this.width, this.height,
				Bitmap.Config.ARGB_8888);
	}

	public void aquireLock() {
		synchronized (this) {
			while (this.locked) {
				try {
					this.wait(100);
				} catch (InterruptedException e) {
				}
			}
			this.locked = true;
		}
	}

	@Override
	public boolean put(byte[] data) {
		if (this.locked)
			return false;

		if (this.in == null)
			return false;

		this.locked = true;
		this.in.put(0, 0, data);
		this.locked = false;

		return true;
	}

	@Override
	public void release() {
		this.aquireLock();
		if (this.in != null) {
			this.in.release();
		}
		if (this.rgb != null) {
			this.rgb.release();
		}
		if (this.out != null) {
			this.out.recycle();
		}
		this.locked = false;
	}

	public Mat rgb() {
		Imgproc.cvtColor(this.in, this.rgb, Imgproc.COLOR_YUV2RGB_NV21);
		return this.rgb;
	}
}