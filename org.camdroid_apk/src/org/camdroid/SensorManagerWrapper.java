package org.camdroid;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class SensorManagerWrapper {

	protected SensorManager mSensorManager;
	protected boolean mEnabled = false;
	protected int mRate;
	protected Sensor mSensor;
	protected SensorEventListener mSensorEventListener;

	public SensorManagerWrapper() {
		super();
	}

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