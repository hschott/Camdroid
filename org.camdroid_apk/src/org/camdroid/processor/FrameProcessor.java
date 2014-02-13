package org.camdroid.processor;

public interface FrameProcessor extends Runnable {

	public abstract boolean put(byte[] data);

	public abstract void allocate(int width, int height);

	public abstract void release();

}