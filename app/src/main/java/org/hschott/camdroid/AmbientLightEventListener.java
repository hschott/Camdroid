package org.hschott.camdroid;

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
		super(context, rate);
	}

	@Override
	public SensorEventListener getSensorEventListener() {
		return new SensorEventListenerImpl();
	}

	@Override
	public Sensor getSensor() {
		return getSensorManager().getDefaultSensor(Sensor.TYPE_LIGHT);
	}

	abstract public void onAmbientLightChanged(float lux);

}
