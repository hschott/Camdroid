package org.hschott.camdroid.processor;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import org.hschott.camdroid.ConfigurationFragment;
import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.hschott.camdroid.R;
import org.opencv.core.Core;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CannyEdgesProcessor extends AbstractOpenCVFrameProcessor {

    private static int min = 96;
    private static int max = 128;

    public static class CannyEdgesUIFragment extends
            ConfigurationFragment {

        @Override
        public int getLayoutId() {
            return R.layout.cannyedges_ui;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = super
                    .onCreateView(inflater, container, savedInstanceState);

            SeekBar minSeekBar = (SeekBar) v.findViewById(R.id.min);
            minSeekBar.setMax(255);
            minSeekBar.setProgress(min);

            minSeekBar
                    .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (fromUser) {
                                min = progress;
                                CannyEdgesUIFragment.this
                                        .showValue(min);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            SeekBar maxSeekBar = (SeekBar) v.findViewById(R.id.max);
            maxSeekBar.setMax(255);
            maxSeekBar.setProgress(max);

            maxSeekBar
                    .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (fromUser) {
                                max = progress;
                                CannyEdgesUIFragment.this
                                        .showValue(max);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            return v;
        }
    }


    public CannyEdgesProcessor(FrameDrawer drawer) {
        super(drawer);
    }

    @Override
    public Fragment getConfigUiFragment(Context context) {
        return Fragment.instantiate(context, CannyEdgesUIFragment.class.getName());
    }

    @Override
    public FrameWorker createFrameWorker() {
        return new CannyEdgesFrameWorker(drawer);
    }

    public class CannyEdgesFrameWorker extends AbstractOpenCVFrameWorker {
        public CannyEdgesFrameWorker(FrameDrawer drawer) {
            super(drawer);
        }

        protected void execute() {
            out = gray();

            Imgproc.blur(out, out, new Size(3, 3));
            Imgproc.Canny(out, out, min, max);
        }

    }
}