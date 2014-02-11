/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camdroid;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.camdriod.R;
import org.camdroid.CameraPreviewView.OnCameraPreviewListener;
import org.camdroid.util.StorageUtils;
import org.camdroid.util.SystemUiHider;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class CameraActivity extends Activity implements OnCameraPreviewListener {
	private SystemUiHider systemUiHider;
	private CameraPreviewView mPreview;
	private ProcessFramesView mProcessorView;

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SS",
			Locale.US);

	private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_SCREEN_OFF)) {
				CameraActivity.this.finish();
			}
		}
	};

	OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.camera) {
				CameraActivity.this.takePicture();
			}
		}
	};

	@Override
	public void onCameraPreviewFrame(byte[] data, int previewFormat) {
	}

	@Override
	public void onCameraPreviewStarted(Camera camera) {
		this.mProcessorView.setEnabled(true);
	}

	@Override
	public void onCameraPreviewStopped() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setResult(RESULT_CANCELED);

		this.systemUiHider = SystemUiHider.getInstance(this,
				SystemUiHider.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES
						| SystemUiHider.FLAG_FULLSCREEN);
		this.systemUiHider.setHideDelayMillis(3000);

		this.setContentView(R.layout.camera);

		this.systemUiHider.hide();

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

		this.mPreview.addCameraFrameListener(this);
		this.mPreview.addCameraFrameListener(this.mProcessorView);
	}

	@Override
	protected void onStop() {
		super.onStop();
		this.mPreview.releaseCamera();

		this.mPreview.removeCameraFrameListener(this);
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
