package org.hschott.camdroid.processor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.hschott.camdroid.R;
import org.hschott.camdroid.UIFragment;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class AdaptiveThresholdProcessor extends AbstractOpenCVFrameProcessor {

    private static int reduction = 0;
    private static int blocksize = 15;

    public static class AdaptiveThresholdUIFragment extends
            ConfigurationFragment implements UIFragment {
        public static AdaptiveThresholdUIFragment newInstance() {
            AdaptiveThresholdUIFragment f = new AdaptiveThresholdUIFragment();
            return f;
        }

        @Override
        public int getLayoutId() {
            return R.layout.adaptivethreshold_ui;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = super
                    .onCreateView(inflater, container, savedInstanceState);

            SeekBar blocksizeSeekBar = (SeekBar) v.findViewById(R.id.blocksize);
            blocksizeSeekBar.setMax(64);
            blocksizeSeekBar.setProgress(blocksize);

            blocksizeSeekBar
                    .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (fromUser) {
                                if (progress % 2 == 0) {
                                    blocksize = progress + 3;
                                } else {
                                    blocksize = progress + 2;
                                }
                                AdaptiveThresholdUIFragment.this
                                        .showValue(blocksize + "px");
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            SeekBar reductionSeekBar = (SeekBar) v.findViewById(R.id.reduction);
            reductionSeekBar.setMax(250);
            reductionSeekBar.setProgress(reduction + 125);

            reductionSeekBar
                    .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (fromUser) {
                                reduction = progress - 125;
                                AdaptiveThresholdUIFragment.this
                                        .showValue(reduction);
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

    public AdaptiveThresholdProcessor(FrameDrawer drawer) {
        super(drawer);
    }

    @Override
    public Fragment getConfigUiFragment() {
        return AdaptiveThresholdUIFragment.newInstance();
    }

    @Override
    public FrameWorker createFrameWorker() {
        return new AdaptiveThresholdFrameWorker(drawer);
    }

    public class AdaptiveThresholdFrameWorker extends AbstractOpenCVFrameWorker {
        public AdaptiveThresholdFrameWorker(FrameDrawer drawer) {
            super(drawer);
        }

        protected void execute() {
            Mat gray = gray();

            Imgproc.adaptiveThreshold(gray, out, 255,
                    Imgproc.THRESH_BINARY_INV, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                    blocksize, reduction);
        }

    }
}