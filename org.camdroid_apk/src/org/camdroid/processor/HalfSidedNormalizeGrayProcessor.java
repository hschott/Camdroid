package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

public class HalfSidedNormalizeGrayProcessor extends AbstractOpenCVFrameProcessor {

	public HalfSidedNormalizeGrayProcessor(FrameDrawer drawer) {
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

			Mat half = gray.submat(0, height, 0, width / 2);
			Core.normalize(half, half, 0, 255, Core.NORM_MINMAX);

			Utils.matToBitmap(gray, out);

			drawer.drawBitmap(out);
		} catch (Exception e) {
		}

		this.locked = false;
	}
}