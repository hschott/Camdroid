package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class PassThroughProcessor extends AbstractOpenCVFrameProcessor {

	private Mat rgb;

	public PassThroughProcessor(FrameDrawer drawer) {
		super(drawer);
	}

	@Override
	public void run() {
		if (this.locked)
			return;

		if (in == null || out == null)
			return;

		this.locked = true;

		try {
			Imgproc.cvtColor(in, rgb, Imgproc.COLOR_YUV2BGR_NV12, 4);

			Utils.matToBitmap(rgb, out);

			drawer.drawBitmap(out);
		} catch (Exception e) {
		}

		this.locked = false;
	}
	
	
	@Override
	public void allocate(int width, int height) {
		super.allocate(width, height);
		rgb = new Mat();
	}

	@Override
	public void release() {
		super.release();
		rgb.release();
	}

}