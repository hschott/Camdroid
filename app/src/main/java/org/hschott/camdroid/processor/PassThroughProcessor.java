package org.hschott.camdroid.processor;

import android.support.v4.app.Fragment;

import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.hschott.camdroid.R;
import org.hschott.camdroid.UIFragment;

public class PassThroughProcessor extends AbstractOpenCVFrameProcessor {

    public static class PassThroughUIFragment extends ConfigurationFragment
            implements UIFragment {
        public static PassThroughUIFragment newInstance() {
            PassThroughUIFragment f = new PassThroughUIFragment();
            return f;
        }

        @Override
        public int getLayoutId() {
            return R.layout.passthrough_ui;
        }

    }

    @Override
    public Fragment getConfigUiFragment() {
        return PassThroughUIFragment.newInstance();
    }

    public PassThroughProcessor(FrameDrawer drawer) {
        super(drawer);
    }

    @Override
    public FrameWorker createFrameWorker() {
        return new PassThroughFrameWorker(drawer);
    }

    public class PassThroughFrameWorker extends AbstractOpenCVFrameWorker {
        public PassThroughFrameWorker(FrameDrawer drawer) {
            super(drawer);
        }

        protected void execute() {
            out = rgb();
       }

    }


}