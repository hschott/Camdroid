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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class ContourDetectionProcessor extends AbstractOpenCVFrameProcessor {

	public static class ContourDetectionUIFragment extends
			ConfigurationFragment implements UIFragment {
		public static ContourDetectionUIFragment newInstance() {
			ContourDetectionUIFragment f = new ContourDetectionUIFragment();
			return f;
		}

		@Override
		public int getLayoutId() {
			return R.layout.contourdetection_ui;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = super
					.onCreateView(inflater, container, savedInstanceState);

			SeekBar minSeekBar = (SeekBar) v.findViewById(R.id.threshold);
			minSeekBar.setMax(255);
			minSeekBar.setProgress(threshold);

			minSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							if (fromUser) {
								threshold = progress;
								ContourDetectionUIFragment.this
										.showValue(threshold);
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

	private static int threshold = 50;

	public ContourDetectionProcessor(FrameDrawer drawer) {
		super(drawer);
	}

	@Override
	public void allocate(int width, int height) {
		super.allocate(width, height);
	}

	@Override
	public Fragment getConfigUiFragment() {
		return ContourDetectionUIFragment.newInstance();
	}

	@Override
	public void release() {
		super.release();
	}

    @Override
    public FrameWorker createFrameWorker() {
        return new ContourDetectionFrameWorker(drawer);
    }

    public class ContourDetectionFrameWorker extends AbstractOpenCVFrameWorker {
        public ContourDetectionFrameWorker(FrameDrawer drawer) {
            super(drawer);
        }

        protected void execute() {

            Mat gray = gray();

            ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            Imgproc.threshold(gray, gray, threshold, 255, Imgproc.THRESH_BINARY);
            Imgproc.findContours(gray, contours, new Mat(),
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

            out = Mat.zeros(gray.height(), gray.width(), CvType.CV_8UC3);
            Imgproc.drawContours(out, contours, -1, new Scalar(255, 255, 255));

        }

    }

}