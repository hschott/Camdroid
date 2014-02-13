package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class SharpenThresholdProcessor extends AbstractOpenCVFrameProcessor {

	private Mat image;
	
	public SharpenThresholdProcessor(FrameDrawer drawer) {
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

			Core.normalize(gray, gray, 15, 255, Core.NORM_MINMAX);

			Imgproc.GaussianBlur(gray, image, new Size(0, 0), 3);
			Core.addWeighted(gray, 1.8, image, -0.8, 0, image);

			Imgproc.adaptiveThreshold(image, image, 255,
					Imgproc.THRESH_BINARY_INV, Imgproc.ADAPTIVE_THRESH_MEAN_C,
					5, 15);

			Utils.matToBitmap(image, out);

			drawer.drawBitmap(out);
		} catch (Exception e) {
		}

		this.locked = false;
	}

	@Override
	public void allocate(int width, int height) {
		super.allocate(width, height);
		image = new Mat();
	}

	@Override
	public void release() {
		super.release();
		image.release();
	}

}