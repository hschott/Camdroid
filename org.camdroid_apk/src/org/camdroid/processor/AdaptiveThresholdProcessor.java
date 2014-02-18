package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.camdroid.R;
import org.camdroid.UIFragment;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

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

public class AdaptiveThresholdProcessor extends AbstractOpenCVFrameProcessor {

	public static class AdaptiveThresholdUIFragment extends Fragment implements
			UIFragment {
		public static AdaptiveThresholdUIFragment newInstance() {
			AdaptiveThresholdUIFragment f = new AdaptiveThresholdUIFragment();
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.adaptivethreshold_ui, null);
			ImageView close = (ImageView) v.findViewById(R.id.close);
			close.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					AdaptiveThresholdUIFragment.this.remove();
				}
			});

			SeekBar blocksizeSeekBar = (SeekBar) v.findViewById(R.id.blocksize);
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
			reductionSeekBar.setProgress(reduction);

			reductionSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							if (fromUser) {
								reduction = progress;
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

	private static int reduction = 15;
	private static int blocksize = 5;

	public AdaptiveThresholdProcessor(FrameDrawer drawer) {
		super(drawer);
	}

	@Override
	public void allocate(int width, int height) {
		super.allocate(width, height);
	}

	@Override
	public Fragment getConfigUiFragment() {
		return AdaptiveThresholdUIFragment.newInstance();
	}

	@Override
	public void release() {
		super.release();
		this.aquireLock();
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
			Mat gray = this.in.submat(0, this.height, 0, this.width);

			Imgproc.adaptiveThreshold(gray, gray, 255,
					Imgproc.THRESH_BINARY_INV, Imgproc.ADAPTIVE_THRESH_MEAN_C,
					blocksize, reduction);

			Utils.matToBitmap(gray, this.out);

			this.drawer.drawBitmap(this.out);
		} catch (Exception e) {
		}

		this.locked = false;
	}

}