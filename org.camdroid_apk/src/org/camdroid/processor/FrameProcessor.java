package org.camdroid.processor;

import android.support.v4.app.Fragment;

public interface FrameProcessor extends Runnable {

	public abstract boolean put(byte[] data);

	public abstract void allocate(int width, int height);

	public abstract void release();

	public Fragment getConfigUiFragment();
}