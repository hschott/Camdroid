package org.hschott.camdroid;

import android.app.Fragment;
import android.content.Context;

import org.hschott.camdroid.processor.FrameWorker;

public interface FrameProcessor {

    public Fragment getConfigUiFragment(Context context);

    public FrameWorker createFrameWorker();

    public boolean put(byte[] data);

    public void allocate(int width, int height);

    public void release();

}