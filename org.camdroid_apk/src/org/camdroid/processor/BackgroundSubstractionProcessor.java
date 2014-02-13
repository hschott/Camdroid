package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;

public class BackgroundSubstractionProcessor extends
		AbstractOpenCVFrameProcessor {

	private Mat mask;
	private BackgroundSubtractorMOG bg;

	public BackgroundSubstractionProcessor(FrameDrawer drawer) {
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
			Mat gray = this.in.submat(0, height, 0, width);
			bg.apply(gray, mask, 0.2);

			Utils.matToBitmap(mask, out);

			drawer.drawBitmap(out);
		} catch (Exception e) {
		}

		this.locked = false;
	}

	@Override
	public void allocate(int width, int height) {
		super.allocate(width, height);
		mask = new Mat();
		bg = new BackgroundSubtractorMOG(4, 3, 0.8);
	}

	@Override
	public void release() {
		super.release();
		mask.release();
	}
}