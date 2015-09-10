package org.hschott.camdroid.processor;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.hschott.camdroid.ConfigurationFragment;
import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.hschott.camdroid.R;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class UnsharpenMaskProcessor extends AbstractOpenCVFrameProcessor {

    public static class UnsharpenMaskUIFragment extends ConfigurationFragment {

        @Override
        public int getLayoutId() {
            return R.layout.unsharpenmask_ui;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = super
                    .onCreateView(inflater, container, savedInstanceState);

            SeekBar sigmaXSeekBar = (SeekBar) v.findViewById(R.id.sigma_x);
            sigmaXSeekBar.setMax(35);
            sigmaXSeekBar.setProgress(sigma_x);

            sigmaXSeekBar
                    .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (progress % 2 == 0) {
                                sigma_x = progress == 0 ? 1 : progress;
                            } else {
                                sigma_x = progress;
                            }
                            if (fromUser) {
                                UnsharpenMaskUIFragment.this.showValue(sigma_x
                                        + "px");
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            SeekBar alphaSeekBar = (SeekBar) v.findViewById(R.id.alpha);
            alphaSeekBar.setMax(50);
            alphaSeekBar.setProgress(alpha);

            alphaSeekBar
                    .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            alpha = progress;
                            if (fromUser) {
                                UnsharpenMaskUIFragment.this
                                        .showValue((double) alpha / 10);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            SeekBar betaSeekBar = (SeekBar) v.findViewById(R.id.beta);
            betaSeekBar.setMax(20);
            betaSeekBar.setProgress(beta);

            betaSeekBar
                    .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            beta = progress;
                            if (fromUser) {
                                UnsharpenMaskUIFragment.this
                                        .showValue(((double) beta - 10) / 10);
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

    private static int sigma_x = 3;
    private static int alpha = 18;
    private static int beta = 2;


    public UnsharpenMaskProcessor(FrameDrawer drawer) {
        super(drawer);
    }


    @Override
    public Fragment getConfigUiFragment(Context context) {
        return Fragment.instantiate(context, UnsharpenMaskUIFragment.class.getName());
    }

    @Override
    public FrameWorker createFrameWorker() {
        return new UnsharpenMaskFrameWorker(drawer);
    }

    public class UnsharpenMaskFrameWorker extends AbstractOpenCVFrameWorker {
        private Mat mask;

        public UnsharpenMaskFrameWorker(FrameDrawer drawer) {
            super(drawer);
        }

        @Override
        public void allocate(int width, int height) {
            super.allocate(width, height);
            this.mask = new Mat();
        }

        @Override
        public void release() {
            super.release();
            this.mask.release();
        }

        protected void execute() {
            out = this.rgb();

            Imgproc.blur(out, this.mask, new Size(sigma_x, sigma_x));
            Core.addWeighted(out, (double) alpha / 10, this.mask,
                    ((double) beta - 10) / 10, 0, out);
        }

    }

}