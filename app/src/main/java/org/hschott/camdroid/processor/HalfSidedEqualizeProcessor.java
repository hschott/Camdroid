package org.hschott.camdroid.processor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.hschott.camdroid.R;
import org.hschott.camdroid.UIFragment;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class HalfSidedEqualizeProcessor extends AbstractOpenCVFrameProcessor {

    public static class HalfSidedNormalizeGrayUIFragment extends
            ConfigurationFragment implements UIFragment {
        public static HalfSidedNormalizeGrayUIFragment newInstance() {
            HalfSidedNormalizeGrayUIFragment f = new HalfSidedNormalizeGrayUIFragment();
            return f;
        }

        @Override
        public int getLayoutId() {
            return R.layout.halfsidednormalizegray_ui;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = super
                    .onCreateView(inflater, container, savedInstanceState);

            return v;
        }
    }

    public HalfSidedEqualizeProcessor(FrameDrawer drawer) {
        super(drawer);
    }

    @Override
    public Fragment getConfigUiFragment() {
        return HalfSidedNormalizeGrayUIFragment.newInstance();
    }

    @Override
    public FrameWorker createFrameWorker() {
        return new HalfSidedEqualizeFrameWorker(drawer);
    }

    public class HalfSidedEqualizeFrameWorker extends AbstractOpenCVFrameWorker {
        public HalfSidedEqualizeFrameWorker(FrameDrawer drawer) {
            super(drawer);
        }

        protected void execute() {
            out = gray();

            Mat half = out.submat(0, this.height, 0, this.width / 2);

            Imgproc.equalizeHist(half, half);
            Core.normalize(half, half, 0, 255, Core.NORM_MINMAX);
        }

    }
}