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

public abstract class AmbientLightEventListener extends SensorManagerWrapper {

	class SensorEventListenerImpl implements SensorEventListener {
		float mAmbientLight = -1;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			float ambientLightLux = event.values[0];
			if (this.mAmbientLight != ambientLightLux) {
				this.mAmbientLight = ambientLightLux;
				AmbientLightEventListener.this
						.onAmbientLightChanged(ambientLightLux);
			}
		}
	}

	public AmbientLightEventListener(Context context) {
		this(context, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public AmbientLightEventListener(Context context, int rate) {
		this.mSensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		this.mRate = rate;
		this.mSensor = this.mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		if (this.mSensor != null) {
			// Create listener only if sensors do exist
			this.mSensorEventListener = new SensorEventListenerImpl();
		}
	}

	abstract public void onAmbientLightChanged(float lux);

}
