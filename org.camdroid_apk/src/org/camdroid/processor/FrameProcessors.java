package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;

public enum FrameProcessors {
	HalfSidedNormalizeGray, SharpenThreshold, ContourDetection, BackgroundSubstraction, PassThrough;

	public FrameProcessor newFrameProcessor(FrameDrawer drawer) {
		switch (ordinal()) {
		case 0:
			return new HalfSidedNormalizeGrayProcessor(drawer);

		case 1:
			return new SharpenThresholdProcessor(drawer);

		case 2:
			return new ContourDetectionProcessor(drawer);
			
		case 3:
			return new BackgroundSubstractionProcessor(drawer);

		case 4:
			return new PassThroughProcessor(drawer);

		default:
			return null;
		}
	}
}
