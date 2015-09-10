package org.hschott.camdroid.processor;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import org.hschott.camdroid.ConfigurationFragment;
import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.hschott.camdroid.R;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;

public class NormalizeGrayProcessor extends AbstractOpenCVFrameProcessor {

    private static int min = 0;
    private static int max = 255;

    public static class NormalizeGrayUIFragment extends
            ConfigurationFragment {

        @Override
        public int getLayoutId() {
            return R.layout.normalizegray_ui;
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
                                NormalizeGrayUIFragment.this
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
                                NormalizeGrayUIFragment.this
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


    public NormalizeGrayProcessor(FrameDrawer drawer) {
        super(drawer);
    }

    @Override
    public Fragment getConfigUiFragment(Context context) {
        return Fragment.instantiate(context, NormalizeGrayUIFragment.class.getName());
    }

    @Override
    public FrameWorker createFrameWorker() {
        return new NormalizeGrayFrameWorker(drawer);
    }

    public class NormalizeGrayFrameWorker extends AbstractOpenCVFrameWorker {
        public NormalizeGrayFrameWorker(FrameDrawer drawer) {
            super(drawer);
        }

        protected void execute() {
            out = gray();

            Imgproc.equalizeHist(out, out);
            Core.normalize(out, out, min, max, Core.NORM_MINMAX);
        }

    }
}