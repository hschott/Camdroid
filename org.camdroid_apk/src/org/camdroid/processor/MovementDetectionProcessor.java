package org.camdroid.processor;

import java.util.ArrayList;

import org.camdriod.R;
import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.camdroid.UIFragment;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MovementDetectionProcessor extends AbstractOpenCVFrameProcessor {
	public static class BackgroundSubstractionUIFragment extends Fragment
			implements UIFragment {

		public static BackgroundSubstractionUIFragment newInstance() {
			BackgroundSubstractionUIFragment f = new BackgroundSubstractionUIFragment();
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.movementdetection_ui, null);
			ImageView close = (ImageView) v.findViewById(R.id.close);
			close.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					BackgroundSubstractionUIFragment.this.remove();
				}
			});

			SeekBar learningRateSeekBar = (SeekBar) v
					.findViewById(R.id.learning_rate);
			learningRateSeekBar.setProgress(learning_rate);

			learningRateSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							if (fromUser) {
								learning_rate = progress;
							}
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					});

			SeekBar objectSizeSeekBar = (SeekBar) v
					.findViewById(R.id.object_size);
			objectSizeSeekBar.setProgress(object_size);

			objectSizeSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							if (fromUser) {
								object_size = progress;
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

		@Override
		public void remove() {
			FragmentActivity activity = this.getActivity();
			if (activity != null) {
				activity.getSupportFragmentManager().beginTransaction()
						.remove(this).commit();
			}
		}
	}

	private static int object_size = 25;
	private static int learning_rate = 5;

	private Mat mask;

	private BackgroundSubtractorMOG mog;

	public MovementDetectionProcessor(FrameDrawer drawer) {
		super(drawer);
	}

	@Override
	public void allocate(int width, int height) {
		super.allocate(width, height);
		this.mask = new Mat();
		this.mog = new BackgroundSubtractorMOG();
	}

	@Override
	public Fragment getConfigUiFragment() {
		return BackgroundSubstractionUIFragment.newInstance();
	}

	@Override
	public void release() {
		super.release();
		this.aquireLock();
		this.mask.release();
		this.locked = false;
	}

	@Override
	public void run() {
		if (this.locked)
			return;

		if (this.in == null || this.out == null)
			return;

		this.locked = true;

		try {
			Mat rgb = this.rgb();

			this.mog.apply(rgb, this.mask, (double) learning_rate / 1000);

			Mat erode = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE,
					new Size(2, 2), new Point(-1, -1));
			Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE,
					new Size(2, 2), new Point(-1, -1));

			Imgproc.erode(this.mask, this.mask, erode, new Point(-1, -1), 3);
			Imgproc.dilate(this.mask, this.mask, dilate, new Point(-1, -1), 3);

			ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			Imgproc.findContours(this.mask, contours, new Mat(),
					Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

			long fullSize = this.in.width() * this.in.height();

			for (MatOfPoint matOfPoint : contours) {
				Rect r = Imgproc.boundingRect(matOfPoint);
				if (r.area() > fullSize * ((double) object_size / 2000)) {
					Core.rectangle(rgb, r.tl(), r.br(), new Scalar(255, 0, 0),
							1);
				}
			}

			Utils.matToBitmap(rgb, this.out);

			this.drawer.drawBitmap(this.out);
		} catch (Exception e) {
		}

		this.locked = false;
	}
}