package org.camdroid;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.camdroid.processor.FrameProcessor;
import org.camdroid.processor.FrameProcessors;
import org.camdroid.util.StorageUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

public class CameraActivity extends ActionBarActivity {
	private CameraPreviewView mPreview;
	private ProcessFramesView mProcessorView;
	private Fragment mConfigUIFragment;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setResult(RESULT_CANCELED);

		this.setContentView(R.layout.camera);

		this.mPreview = (CameraPreviewView) this
				.findViewById(R.id.camera_surface_view);

		this.mProcessorView = (ProcessFramesView) this
				.findViewById(R.id.processor_view);

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// Catch screen off event
		IntentFilter f = new IntentFilter();
		f.addAction(Intent.ACTION_SCREEN_OFF);
		this.registerReceiver(this.screenOffReceiver, f);

	};

	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		MenuItem i1 = menu
				.add(Menu.NONE, Menu.NONE, Menu.NONE,
						R.string.configure_processor)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						FrameProcessor frameProcessor = CameraActivity.this.mProcessorView
								.getFrameProcessor();

						if (frameProcessor != null) {
							CameraActivity.this.mConfigUIFragment = frameProcessor
									.getConfigUiFragment();
							FragmentManager fm = CameraActivity.this
									.getSupportFragmentManager();
							FragmentTransaction ft = fm.beginTransaction();
							ft.replace(R.id.config_container,
									CameraActivity.this.mConfigUIFragment, null);
							ft.show(CameraActivity.this.mConfigUIFragment);
							ft.commit();
						}

						return true;
					}
				});
		MenuItemCompat
				.setShowAsAction(i1, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

		MenuItem i2 = menu
				.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.take_picture)
				.setIcon(android.R.drawable.ic_menu_camera)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						CameraActivity.this.takePicture();
						return true;
					}
				});
		MenuItemCompat
				.setShowAsAction(i2, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

		for (final FrameProcessors f : FrameProcessors.values()) {
			menu.add(Menu.NONE, f.ordinal(), Menu.NONE, f.name())
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							if (CameraActivity.this.mConfigUIFragment != null) {
								((UIFragment) CameraActivity.this.mConfigUIFragment)
										.remove();
							}
							CameraActivity.this.setFrameProcessor(f.ordinal());
							return true;
						}
					});
		}
		return true;
	};

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

		Toast.makeText(this.getApplicationContext(), R.string.select,
				Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onStop() {
		super.onStop();
		this.mPreview.releaseCamera();

		this.mPreview.removeCameraFrameListener(this.mProcessorView);
	}

	protected void setFrameProcessor(int ordinal) {
		FrameProcessors t = FrameProcessors.values()[ordinal];
		this.mPreview.stopPreview();
		this.mProcessorView.setFrameProcessor(t
				.newFrameProcessor(this.mProcessorView));
		this.mPreview.startPreview();
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

				Toast.makeText(CameraActivity.this.getApplicationContext(),
						R.string.picture_saved, Toast.LENGTH_SHORT).show();

				CameraActivity.this.mPreview.stopPreview();
				CameraActivity.this.mPreview.startPreview();
			}
		};

		this.mPreview.takePicture(callback);
	}
}
