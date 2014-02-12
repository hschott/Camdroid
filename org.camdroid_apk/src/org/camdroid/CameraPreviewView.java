package org.camdroid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.camdroid.CameraPreviewView.OnCameraPreviewListener.AutoFocusManagerAware;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered
 * preview of the Camera to the surface. We need to center the SurfaceView
 * because not all devices have cameras that support preview sizes at the same
 * aspect ratio as the device's display.
 */
public class CameraPreviewView extends ViewGroup implements PreviewCallback,
		SurfaceHolder.Callback {

	public interface OnCameraPreviewListener {
		public interface AutoFocusManagerAware {
			public AutoFocusManager getAutoFocusManager();

			public void setAutoFocusManager(AutoFocusManager autoFocusManager);
		}

		public void onCameraPreviewFrame(byte[] data, int previewFormat);

		public void onCameraPreviewStarted(Camera camera);

		public void onCameraPreviewStopped();
	};

	private static final String TAG = CameraPreviewView.class.getSimpleName();

	private SurfaceView mSurfaceView;
	private Camera mCamera;
	private AutoFocusManager mAutoFocusManager;

	private List<OnCameraPreviewListener> mCameraFrameListeners = new ArrayList<OnCameraPreviewListener>();

	private Size mPreviewSize;
	private int mPreviewFormat;

	private boolean previewRunning = false;

	private OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (CameraPreviewView.this.previewRunning) {
				if (CameraPreviewView.this.mAutoFocusManager != null) {
					CameraPreviewView.this.mAutoFocusManager.focus();
				}
			}
		}
	};

	private boolean configured = false;

	public CameraPreviewView(Context context) {
		super(context);
		this.initView(context);
	}

	public CameraPreviewView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.initView(context);
	}

	public CameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.initView(context);
	}

	public void addCameraFrameListener(
			OnCameraPreviewListener cameraFrameListener) {
		if (cameraFrameListener instanceof AutoFocusManagerAware) {
			AutoFocusManagerAware target = (AutoFocusManagerAware) cameraFrameListener;
			target.setAutoFocusManager(this.mAutoFocusManager);
		}
		this.mCameraFrameListeners.add(cameraFrameListener);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public void createCamera() {
		Log.d(TAG, "createCamera()");
		// Open the default i.e. the first rear facing camera.
		int cameraIndex = CameraInterface.firstBackFacingCamera();
		this.mCamera = CameraInterface.openCamera(cameraIndex);

		if (this.mCamera == null)
			return;

		CameraManager.initializeCamera(this.mCamera);
		Log.d(TAG, "camera created");

		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(cameraIndex, cameraInfo);

		boolean canDisableSystemShutterSound = false;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			if (cameraInfo.canDisableShutterSound) {
				canDisableSystemShutterSound = this.mCamera
						.enableShutterSound(false);
			}
		}

		this.mAutoFocusManager = new AutoFocusManager(this.getContext()
				.getApplicationContext(), this.mCamera,
				canDisableSystemShutterSound);

	}

	private void initView(Context context) {
		Log.d(TAG, "initView()");
		this.setBackgroundColor(this.getResources().getColor(
				android.R.color.black));
		this.mSurfaceView = new SurfaceView(context);
		this.mSurfaceView.setKeepScreenOn(true);

		this.addView(this.mSurfaceView, 0);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		SurfaceHolder holder = this.mSurfaceView.getHolder();
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		holder.addCallback(this);

		this.setOnClickListener(this.onClickListener);

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (this.getChildCount() > 0) {
			final View child = this.getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;
			if (this.mPreviewSize != null) {
				previewWidth = this.mPreviewSize.width;
				previewHeight = this.mPreviewSize.height;
			}
			Log.d(TAG, "onLayout() w=" + previewWidth + " h=" + previewHeight);

			// Center the child SurfaceView within the parent.
			if (width * previewHeight > height * previewWidth) {
				final int scaledChildWidth = previewWidth * height
						/ previewHeight;
				child.layout((width - scaledChildWidth) / 2, 0,
						(width + scaledChildWidth) / 2, height);
			} else {
				final int scaledChildHeight = previewHeight * width
						/ previewWidth;
				child.layout(0, (height - scaledChildHeight) / 2, width,
						(height + scaledChildHeight) / 2);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// We purposely disregard child measurements because act as a
		// wrapper to a SurfaceView that centers the camera preview instead
		// of stretching it.
		final int width = resolveSize(this.getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(this.getSuggestedMinimumHeight(),
				heightMeasureSpec);
		this.setMeasuredDimension(width, height);
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		for (OnCameraPreviewListener cameraFrameListener : this.mCameraFrameListeners) {
			cameraFrameListener.onCameraPreviewFrame(data, this.mPreviewFormat);
		}
	}

	public void releaseCamera() {
		Log.d(TAG, "releaseCamera()");
		if (this.mCamera != null) {
			if (this.mAutoFocusManager != null) {
				this.mAutoFocusManager.stop();
				this.mAutoFocusManager = null;
			}

			this.mCamera.release();

			Log.d(TAG, "camera released");

			this.configured = false;
			this.mCamera = null;
		}
	}

	public void removeCameraFrameListener(
			OnCameraPreviewListener cameraFrameListener) {
		this.mCameraFrameListeners.remove(cameraFrameListener);
	}

	public void startPreview() {
		Log.d(TAG, "startPreview() previewRunning=" + this.previewRunning
				+ ", configured=" + this.configured);
		if (this.mCamera != null && !this.previewRunning && this.configured) {
			this.previewRunning = true;
			this.mCamera.setPreviewCallback(this);
			this.mCamera.startPreview();
			if (this.mAutoFocusManager != null) {
				this.mAutoFocusManager.resume();
			}
			for (OnCameraPreviewListener cameraFrameListener : this.mCameraFrameListeners) {
				cameraFrameListener.onCameraPreviewStarted(this.mCamera);
			}
			Log.d(TAG, "camera preview started");
		}
	}

	public void stopPreview() {
		Log.d(TAG, "stopPreview() previewRunning=" + this.previewRunning);
		if (this.mCamera != null && this.previewRunning) {
			if (this.mAutoFocusManager != null) {
				this.mAutoFocusManager.pause();
			}
			this.mCamera.setPreviewCallback(null);
			this.mCamera.stopPreview();
			for (OnCameraPreviewListener cameraFrameListener : this.mCameraFrameListeners) {
				cameraFrameListener.onCameraPreviewStopped();
			}
			this.previewRunning = false;
			Log.d(TAG, "camera preview stopped");
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.d(TAG, "surfaceChanged()");
		if (this.mCamera != null && holder.getSurface() != null) {
			this.stopPreview();
			Camera.Parameters parameters = this.mCamera.getParameters();

			CameraManager.initPreviewSize(parameters,
					parameters.getPictureSize(), w, h);

			this.mCamera.setParameters(parameters);

			parameters = this.mCamera.getParameters();
			this.mPreviewFormat = parameters.getPreviewFormat();
			this.mPreviewSize = parameters.getPreviewSize();
			try {
				this.mCamera.setPreviewDisplay(holder);
				this.configured = true;
			} catch (IOException exception) {
				Log.e(TAG, "IOException caused by setPreviewDisplay()",
						exception);
			}

			this.requestLayout();
			this.startPreview();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		this.stopPreview();
	}

	public void takePicture(PictureCallback callback) {
		if (this.mAutoFocusManager != null) {
			this.mAutoFocusManager.takePicture(callback);
		}
	}

}