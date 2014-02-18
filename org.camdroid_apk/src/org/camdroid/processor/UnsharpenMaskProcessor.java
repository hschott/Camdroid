package org.camdroid.processor;

import org.camdriod.R;
import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.camdroid.UIFragment;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
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

public class UnsharpenMaskProcessor extends AbstractOpenCVFrameProcessor {

	public static class UnsharpenMaskUIFragment extends Fragment implements
			UIFragment {
		public static UnsharpenMaskUIFragment newInstance() {
			UnsharpenMaskUIFragment f = new UnsharpenMaskUIFragment();
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.unsharpenmask_ui, null);
			ImageView close = (ImageView) v.findViewById(R.id.close);
			close.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					UnsharpenMaskUIFragment.this.remove();
				}
			});

			SeekBar sigmaXSeekBar = (SeekBar) v.findViewById(R.id.sigma_x);
			sigmaXSeekBar.setProgress(kernel_size);

			sigmaXSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							if (fromUser) {
								if (progress % 2 == 0) {
									kernel_size = progress + 1;
								} else {
									kernel_size = progress;
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

			SeekBar alphaSeekBar = (SeekBar) v.findViewById(R.id.alpha);
			alphaSeekBar.setProgress(alpha);

			alphaSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							if (fromUser) {
								alpha = progress;
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
			betaSeekBar.setProgress(beta);

			betaSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							if (fromUser) {
								beta = progress;
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

	private static int kernel_size = 3;
	private static int alpha = 18;
	private static int beta = 2;

	private Mat mask;

	public UnsharpenMaskProcessor(FrameDrawer drawer) {
		super(drawer);
	}

	@Override
	public void allocate(int width, int height) {
		super.allocate(width, height);
		this.mask = new Mat();
	}

	@Override
	public Fragment getConfigUiFragment() {
		return UnsharpenMaskUIFragment.newInstance();
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

			Imgproc.blur(rgb, this.mask, new Size(kernel_size, kernel_size));
			Core.addWeighted(rgb, (double) alpha / 10, this.mask,
					((double) beta - 10) / 10, 0, rgb);

			Utils.matToBitmap(rgb, this.out);

			this.drawer.drawBitmap(this.out);
		} catch (Exception e) {
		}

		this.locked = false;
	}
}