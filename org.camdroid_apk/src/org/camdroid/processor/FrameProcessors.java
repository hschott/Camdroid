package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;

public enum FrameProcessors {
	EqualizeGray, AdaptiveThreshold, ContourDetection, MovementDetection, UnsharpenMask, PassThrough;

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

		default:
			return new PassThroughProcessor(drawer);
		}
	}
}
