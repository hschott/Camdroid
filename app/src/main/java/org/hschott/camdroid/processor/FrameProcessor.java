package org.hschott.camdroid.processor;

import android.support.v4.app.Fragment;

public interface FrameProcessor {

    public Fragment getConfigUiFragment();

    public FrameWorker createFrameWorker();

    public boolean put(byte[] data);

    public void allocate(int width, int height);

    public void release();

}