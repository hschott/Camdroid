package org.hschott.camdroid;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class SensorManagerWrapper {

	private SensorManager mSensorManager;
	private boolean mEnabled = false;
	private int mRate;
	private Sensor mSensor;
	private SensorEventListener mSensorEventListener;

	public SensorManagerWrapper(Context context, int rate) {
		super();
		mRate = rate;
		this.mSensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		this.mSensor = getSensor();
		if (this.mSensor != null) {
			// Create listener only if sensors do exist
			this.mSensorEventListener = getSensorEventListener();
		}
	}

	public SensorManager getSensorManager() {
		return mSensorManager;
	}

	public abstract Sensor getSensor();

	public abstract SensorEventListener getSensorEventListener();

	/**
	 * Disables the SensorEventListener.
	 */
	public void disable() {
		if (this.mSensor == null)
			return;
		if (this.mEnabled == true) {
			this.mSensorManager.unregisterListener(this.mSensorEventListener);
			this.mEnabled = false;
		}
	}

	/**
	 * Enables the SensorEventListener so it will monitor the sensor
	 */
	public void enable() {
		if (this.mSensor == null)
			return;
		if (this.mEnabled == false) {
			this.mSensorManager.registerListener(this.mSensorEventListener,
					this.mSensor, this.mRate);
			this.mEnabled = true;
		}
	}

	public boolean isEnabled() {
		return this.mEnabled;
	}

}