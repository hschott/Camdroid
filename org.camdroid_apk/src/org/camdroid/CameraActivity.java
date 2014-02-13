package org.camdroid;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.camdriod.R;
import org.camdroid.processor.FrameProcessors;
import org.camdroid.util.StorageUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;

public class CameraActivity extends ActionBarActivity {
	private CameraPreviewView mPreview;
	private ProcessFramesView mProcessorView;

	private SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd_HH-mm-ss-SS", Locale.US);

	private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_SCREEN_OFF)) {
				CameraActivity.this.finish();
			}
		}
	};

	private OnClickListener onClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.camera) {
				CameraActivity.this.takePicture();
			}
		}
	};

	public boolean onOptionsItemSelected(MenuItem item) {
		setFrameProcessor(item.getItemId());
		return true;
	};

	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		for (FrameProcessors f : FrameProcessors.values()) {
			menu.add(Menu.NONE, f.ordinal(), Menu.NONE, f.name());
		}
		return true;
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setResult(RESULT_CANCELED);

		this.setContentView(R.layout.camera);

		this.mPreview = (CameraPreviewView) this
				.findViewById(R.id.camera_surface_view);

		this.mProcessorView = (ProcessFramesView) this
				.findViewById(R.id.processor_view);

		LinearLayout v = (LinearLayout) this.findViewById(R.id.camera);

		v.setOnClickListener(this.onClickListener);

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// Catch screen off event
		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_SCREEN_OFF);
		this.registerReceiver(this.screenOffReceiver, f);

	}

	protected void setFrameProcessor(int ordinal) {
		FrameProcessors t = FrameProcessors.values()[ordinal];
		mPreview.stopPreview();
		mProcessorView.setFrameProcessor(t.newFrameProcessor(mProcessorView));
		mPreview.startPreview();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(this.screenOffReceiver);
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.mPreview.stopPreview();
	}

	@Override
	protected void onResume() {
		super.onResume();

		this.mPreview.startPreview();
	}

	@Override
	protected void onStart() {
		super.onStart();
		this.mPreview.createCamera();

		this.mPreview.addCameraFrameListener(this.mProcessorView);
		
		Toast.makeText(getApplicationContext(), R.string.select, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onStop() {
		super.onStop();
		this.mPreview.releaseCamera();

		this.mPreview.removeCameraFrameListener(this.mProcessorView);
	}

	public void takePicture() {
		Camera.PictureCallback callback = new Camera.PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				Date date = new Date();
				String formated = CameraActivity.this.sdf.format(date);

				String filename = StorageUtils
						.getStorageDirectory(
								CameraActivity.this.getApplicationContext(),
								"Camdroid")
						+ "/" + formated + "_" + "CAPTURE" + ".jpg";

				File file = new File(filename);
				StorageUtils.writeFile(file, data);
				StorageUtils.updateMedia(
						CameraActivity.this.getApplicationContext(), file);

				Toast.makeText(getApplicationContext(), R.string.picture_saved,
						Toast.LENGTH_SHORT).show();

				mPreview.stopPreview();
				mPreview.startPreview();
			}
		};

		this.mPreview.takePicture(callback);
	}
}
