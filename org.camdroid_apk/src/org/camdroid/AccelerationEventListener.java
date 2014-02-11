/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public abstract class AccelerationEventListener extends SensorManagerWrapper {

	class SensorEventListenerImpl implements SensorEventListener {

		float mAccelaration = ACCELERATION_UNKNOWN;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {

			float[] values = event.values;
			float X = values[0];
			float Y = values[1];
			float Z = values[2];

			float acceleration = (float) Math.sqrt(X * X + Y * Y + Z * Z);

			if (acceleration != this.mAccelaration) {
				this.mAccelaration = acceleration;
				AccelerationEventListener.this
						.onAccelerationChanged(acceleration);
			}
		}
	}

	public static final int ACCELERATION_UNKNOWN = -1;
	private static final String TAG = "AccelerationEventListener";

	public AccelerationEventListener(Context context) {
		this(context, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public AccelerationEventListener(Context context, int rate) {
		this.mSensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		this.mRate = rate;
		this.mSensor = this.mSensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		if (this.mSensor != null) {
			// Create listener only if sensors do exist
			this.mSensorEventListener = new SensorEventListenerImpl();
		}
	}

	/**
	 * Disables the AccelerationEventListener.
	 */
	@Override
	public void disable() {
		if (this.mSensor == null) {
			Log.w(TAG, "Cannot detect sensors. Invalid disable");
			return;
		}
		if (this.mEnabled == true) {
			this.mSensorManager.unregisterListener(this.mSensorEventListener);
			this.mEnabled = false;
		}
	}

	/**
	 * Enables the AccelerationEventListener so it will monitor the sensor and
	 * call {@link #onOrientationChanged} when the device orientation changes.
	 */
	@Override
	public void enable() {
		if (this.mSensor == null) {
			Log.w(TAG, "Cannot detect sensors. Not enabled");
			return;
		}
		if (this.mEnabled == false) {
			this.mSensorManager.registerListener(this.mSensorEventListener,
					this.mSensor, this.mRate);
			this.mEnabled = true;
		}
	}

	@Override
	public boolean isEnabled() {
		return this.mEnabled;
	}

	abstract public void onAccelerationChanged(double orientation);

}
