package org.camdroid.processor;

import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.camdroid.R;
import org.camdroid.UIFragment;
import org.opencv.android.Utils;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

public class PassThroughProcessor extends AbstractOpenCVFrameProcessor {

	public static class PassThroughUIFragment extends Fragment implements
			UIFragment {
		public static PassThroughUIFragment newInstance() {
			PassThroughUIFragment f = new PassThroughUIFragment();
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.passthrough_ui, null);
			ImageView close = (ImageView) v.findViewById(R.id.close);
			close.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					PassThroughUIFragment.this.remove();
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

	public PassThroughProcessor(FrameDrawer drawer) {
		super(drawer);
	}

	@Override
	public void allocate(int width, int height) {
		super.allocate(width, height);
	}

	@Override
	public Fragment getConfigUiFragment() {
		return PassThroughUIFragment.newInstance();
	}

	@Override
	public void release() {
		super.release();
	}

	@Override
	public void run() {
		if (this.locked)
			return;

		if (this.in == null || this.out == null)
			return;

		this.locked = true;

		try {
			Utils.matToBitmap(this.rgb(), this.out);

			this.drawer.drawBitmap(this.out);
		} catch (Exception e) {
		}

		this.locked = false;
	}

}