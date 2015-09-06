package org.hschott.camdroid.processor;

import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;

public enum FrameProcessors {
	EqualizeGray, AdaptiveThreshold, ContourDetection, MovementDetection, UnsharpenMask, CascadeClassifier, PassThrough;

	public FrameProcessor newFrameProcessor(FrameDrawer drawer) {
		switch (this.ordinal()) {
		case 0:
			return new HalfSidedEqualizeProcessor(drawer);

		case 1:
			return new AdaptiveThresholdProcessor(drawer);

		case 2:
			return new ContourDetectionProcessor(drawer);

		case 3:
			return new MovementDetectionProcessor(drawer);

		case 4:
			return new UnsharpenMaskProcessor(drawer);

		case 5:
			return new CascadeClassifierProcessor(drawer);

		default:
			return new PassThroughProcessor(drawer);
		}
	}
}
