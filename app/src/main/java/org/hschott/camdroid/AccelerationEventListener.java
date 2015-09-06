package org.hschott.camdroid;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

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

	public AccelerationEventListener(Context context) {
		this(context, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public AccelerationEventListener(Context context, int rate) {
		super(context, rate);
	}

	@Override
	public SensorEventListener getSensorEventListener() {
		return new SensorEventListenerImpl();
	}

	@Override
	public Sensor getSensor() {
		return getSensorManager().getDefaultSensor(
				Sensor.TYPE_LINEAR_ACCELERATION);
	}

	abstract public void onAccelerationChanged(double orientation);

}
