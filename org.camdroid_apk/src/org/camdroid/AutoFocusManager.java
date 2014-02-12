package org.camdroid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.camdriod.R;
import org.camdroid.util.Utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

public final class AutoFocusManager implements Camera.AutoFocusCallback,
		Runnable {

	private class FocusAccelerationEventListener extends
			AccelerationEventListener {
		public static final float ACCELERATION_HYSTERESIS = 0.6f;

		public FocusAccelerationEventListener(Context context) {
			super(context);
		}

		public FocusAccelerationEventListener(Context context, int rate) {
			super(context, rate);
		}

		@Override
		public void onAccelerationChanged(double acceleration) {
			if (!AutoFocusManager.this.shouldReFocus) {
				double abs = Math.abs(acceleration);
				AutoFocusManager.this.shouldReFocus = (abs >= ACCELERATION_HYSTERESIS);
			}
		}
	}

	private class FocusAmbientLightEventListener extends
			AmbientLightEventListener {
		private static final float TOO_DARK_LUX = 25.0f;
		private static final float BRIGHT_ENOUGH_LUX = 150.0f;

		public FocusAmbientLightEventListener(Context context) {
			super(context);
		}

		public FocusAmbientLightEventListener(Context context, int rate) {
			super(context, rate);
		}

		@Override
		public void onAmbientLightChanged(float ambientLightLux) {
			if (ambientLightLux <= TOO_DARK_LUX) {
				CameraManager.setTorch(AutoFocusManager.this.camera, true);
			} else if (ambientLightLux >= BRIGHT_ENOUGH_LUX) {
				CameraManager.setTorch(AutoFocusManager.this.camera, false);
			}
		}

	}

	private static final String TAG = AutoFocusManager.class.getSimpleName();

	private static final long AUTO_FOCUS_INTERVAL_MS = 3000L;
	private long autoFocusInterval = AUTO_FOCUS_INTERVAL_MS;

	private final boolean auto_focus_callable;
	private volatile boolean takePicture;
	private volatile boolean focused;
	private volatile boolean active;
	private volatile boolean shouldReFocus = true;

	private final Camera camera;

	private ScheduledExecutorService executor;

	private final SoundManager soundManager;
	private PictureCallback pictureCallback;
	private ShutterCallback shutterCallback;
	private FocusAccelerationEventListener focusAccelerationEventListener;
	private FocusAmbientLightEventListener focusAmbientLightEventListener;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public AutoFocusManager(final Context context, Camera camera,
			final boolean canDisableSystemShutterSound) {
		this.camera = camera;
		this.executor = Executors.newSingleThreadScheduledExecutor();

		this.focusAccelerationEventListener = new FocusAccelerationEventListener(
				context, SensorManager.SENSOR_DELAY_GAME);
		this.focusAmbientLightEventListener = new FocusAmbientLightEventListener(
				context, SensorManager.SENSOR_DELAY_GAME);

		String focusMode = this.camera.getParameters().getFocusMode();
		this.auto_focus_callable = Camera.Parameters.FOCUS_MODE_AUTO
				.equals(focusMode)
				|| Camera.Parameters.FOCUS_MODE_MACRO.equals(focusMode);

		List<Integer> resId = new ArrayList<Integer>();
		if (auto_focus_callable) {
			resId.add(R.raw.beep);
		}
		if (canDisableSystemShutterSound) {
			resId.add(R.raw.shutter);
		}

		this.soundManager = new SoundManager(context, Utils.toIntArray(resId));

		if (canDisableSystemShutterSound) {
			this.shutterCallback = new ShutterCallback() {
				@Override
				public void onShutter() {
					AutoFocusManager.this.soundManager.play(R.raw.shutter);
				}
			};
		}

	}

	public synchronized void focus() {
		if (this.active && this.auto_focus_callable) {
			try {
				this.camera.autoFocus(this);
				this.focused = false;
			} catch (RuntimeException re) {
				Log.w(TAG, "Unexpected exception while focusing", re);
			}
		}
	}

	public synchronized boolean isFocused() {
		return this.focused || !this.auto_focus_callable;
	}

	@Override
	public synchronized void onAutoFocus(boolean success, Camera theCamera) {
		this.focused = success;

		if (this.focused) {
			this.soundManager.play(R.raw.beep);

			if (this.focusAccelerationEventListener.isEnabled()) {
				this.shouldReFocus = false;
			} else {
				this.shouldReFocus = true;
			}
		}

		if (this.takePicture && this.focused) {
			try {
				this.camera.setPreviewCallback(null);
				this.camera.takePicture(this.shutterCallback, null,
						this.pictureCallback);
				this.takePicture = false;
			} catch (RuntimeException re) {
				Log.w(TAG, "Unexpected exception while delayed takePicture", re);
			}
		} else {
			this.executor.schedule(this, this.autoFocusInterval,
					TimeUnit.MILLISECONDS);
		}
	}

	public synchronized void pause() {
		this.active = false;
		this.focusAccelerationEventListener.disable();
		this.focusAmbientLightEventListener.disable();
	}

	public synchronized void resume() {
		this.active = true;
		if (this.auto_focus_callable) {
			this.focusAccelerationEventListener.enable();
			if (this.focusAccelerationEventListener.isEnabled()) {
				this.autoFocusInterval = 750;
			}
		}
		this.focusAmbientLightEventListener.enable();

		this.executor.execute(this);

		Log.d(TAG, "auto focus call not permitted: " + this.auto_focus_callable);
		Log.d(TAG, "acceleration listener enabled: "
				+ this.focusAccelerationEventListener.isEnabled());
		Log.d(TAG, "ambientlight listener enabled: "
				+ this.focusAmbientLightEventListener.isEnabled());
	}

	@Override
	public synchronized void run() {
		if (this.active && this.auto_focus_callable) {
			if (this.shouldReFocus) {
				try {
					this.camera.autoFocus(this);
					this.focused = false;
				} catch (RuntimeException re) {
					Log.w(TAG, "Unexpected exception while focusing", re);
					this.executor.schedule(this, this.autoFocusInterval,
							TimeUnit.MILLISECONDS);
				}
			} else {
				this.executor.schedule(this, this.autoFocusInterval,
						TimeUnit.MILLISECONDS);
			}
		}
	}

	public synchronized void stop() {
		this.active = false;
		this.takePicture = false;
		this.focused = false;
		this.pictureCallback = null;

		this.soundManager.release();

		this.executor.shutdownNow();

		this.focusAccelerationEventListener.disable();
		this.focusAmbientLightEventListener.disable();

		if (this.auto_focus_callable) {
			try {
				this.camera.cancelAutoFocus();
			} catch (RuntimeException re) {
				Log.w(TAG, "Unexpected exception while cancelling focusing", re);
			}
		}

	}

	public synchronized void takePicture(PictureCallback callback) {
		if (this.isFocused()) {
			try {
				this.camera.setPreviewCallback(null);
				this.camera.takePicture(this.shutterCallback, null, callback);
			} catch (RuntimeException re) {
				Log.w(TAG, "Unexpected exception while takePicture", re);
			}
		} else {
			this.pictureCallback = callback;
			this.takePicture = true;
		}
	}
}
