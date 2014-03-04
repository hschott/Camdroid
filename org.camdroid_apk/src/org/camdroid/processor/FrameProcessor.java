package org.camdroid.processor;

import android.support.v4.app.Fragment;

public interface FrameProcessor extends Runnable {

	public abstract void allocate(int width, int height);

	public Fragment getConfigUiFragment();

	public abstract boolean put(byte[] data);

	public abstract void release();

	public abstract void store(String filename);
}