package org.camdroid.processor;

import java.util.ArrayList;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ContourDetectionProcessor extends AbstractOpenCVFrameProcessor {

	public ContourDetectionProcessor(FrameDrawer drawer) {
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
			Imgproc.threshold(gray, gray, 50, 180, Imgproc.THRESH_BINARY);

			ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();

			Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_LIST,
					Imgproc.CHAIN_APPROX_SIMPLE);

			Mat zeros = Mat.zeros(gray.height(), gray.width(), CvType.CV_8UC3);
			Imgproc.drawContours(zeros, contours, -1, new Scalar(255, 255, 255));

			Utils.matToBitmap(zeros, out);

			drawer.drawBitmap(out);
		} catch (Exception e) {
		}

		this.locked = false;
	}

	@Override
	public void allocate(int width, int height) {
		super.allocate(width, height);
	}

	@Override
	public void release() {
		super.release();
	}

}