package org.hschott.camdroid;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public interface OnCameraPreviewListener {
	public interface AutoFocusManagerAware {
		public AutoFocusManager getAutoFocusManager();

		public void setAutoFocusManager(AutoFocusManager autoFocusManager);
	}

	public interface FrameDrawer {
		public void drawBitmap(Bitmap bitmap);

		public DisplayMetrics getDisplayMetrics();
	}

	public void onCameraPreviewFrame(byte[] data, int previewFormat);

	public void onCameraPreviewStarted(Camera camera);

	public void onCameraPreviewStopped();
}