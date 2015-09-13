package org.hschott.camdroid;

import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.hschott.camdroid.processor.AdaptiveThresholdProcessor;
import org.hschott.camdroid.processor.CannyEdgesProcessor;
import org.hschott.camdroid.processor.CascadeClassifierProcessor;
import org.hschott.camdroid.processor.NormalizeGrayProcessor;
import org.hschott.camdroid.processor.MovementDetectionProcessor;
import org.hschott.camdroid.processor.ColorSpaceProcessor;
import org.hschott.camdroid.processor.OCRProcessor;
import org.hschott.camdroid.processor.UnsharpenMaskProcessor;

public enum FrameProcessors {
    ColorSpace, NormalizeGray, AdaptiveThreshold, CannyEdges, UnsharpenMask, MovementDetection, CascadeClassifier, OCR;

    public FrameProcessor newFrameProcessor(FrameDrawer drawer) {
        switch (this.ordinal()) {
            case 0:
                return new ColorSpaceProcessor(drawer);

            case 1:
                return new NormalizeGrayProcessor(drawer);

            case 2:
                return new AdaptiveThresholdProcessor(drawer);

            case 3:
                return new CannyEdgesProcessor(drawer);

            case 4:
                return new UnsharpenMaskProcessor(drawer);

            case 5:
                return new MovementDetectionProcessor(drawer);

            case 6:
                return new CascadeClassifierProcessor(drawer);

            case 7:
                return new OCRProcessor(drawer);

            default:
                return new ColorSpaceProcessor(drawer);
        }
    }
}
