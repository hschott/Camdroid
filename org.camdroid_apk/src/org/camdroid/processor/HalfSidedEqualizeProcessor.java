package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.camdroid.R;
import org.camdroid.UIFragment;
import org.opencv.android.Utils;
import org.opencv.core.Core;
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

public class HalfSidedEqualizeProcessor extends AbstractOpenCVFrameProcessor {

	public static class HalfSidedNormalizeGrayUIFragment extends Fragment
			implements UIFragment {
		public static HalfSidedNormalizeGrayUIFragment newInstance() {
			HalfSidedNormalizeGrayUIFragment f = new HalfSidedNormalizeGrayUIFragment();
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.halfsidednormalizegray_ui, null);
			ImageView close = (ImageView) v.findViewById(R.id.close);
			close.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					HalfSidedNormalizeGrayUIFragment.this.remove();
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

	public HalfSidedEqualizeProcessor(FrameDrawer drawer) {
		super(drawer);
	}

	@Override
	public Fragment getConfigUiFragment() {
		return HalfSidedNormalizeGrayUIFragment.newInstance();
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

			Mat half = gray.submat(0, this.height, 0, this.width / 2);

			Imgproc.equalizeHist(half, half);
			Core.normalize(half, half, 0, 255, Core.NORM_MINMAX);

			Utils.matToBitmap(gray, this.out);

			this.drawer.drawBitmap(this.out);
		} catch (Exception e) {
		}

		this.locked = false;
	}
}