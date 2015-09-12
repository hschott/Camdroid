package org.hschott.camdroid;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
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

import org.hschott.camdroid.OnCameraPreviewListener.AutoFocusManagerAware;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered
 * preview of the Camera to the surface. We need to center the SurfaceView
 * because not all devices have cameras that support preview sizes at the same
 * aspect ratio as the device's display.
 */
public class CameraPreviewView extends ViewGroup implements PreviewCallback,
        SurfaceHolder.Callback {

    private static final String TAG = CameraPreviewView.class.getSimpleName();

    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private AutoFocusManager mAutoFocusManager;

    private List<OnCameraPreviewListener> onCameraPreviewListeners = new ArrayList<OnCameraPreviewListener>();

    private Size mPreviewSize;
    private byte[] mBuffer;
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
        this.onCameraPreviewListeners.add(cameraFrameListener);
    }

    public void createCamera() {
        Log.d(TAG, "createCamera()");
        // Open the default i.e. the first rear facing camera.
        int cameraIndex = CameraManager.firstBackFacingCamera();
        this.mCamera = CameraManager.openCamera(cameraIndex);

        if (this.mCamera == null) {
            return;
        }

        CameraManager.initializeCamera(this.mCamera);
        Log.d(TAG, "camera created");

        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraIndex, cameraInfo);

        boolean canDisableSystemShutterSound = false;

        if (cameraInfo.canDisableShutterSound) {
            canDisableSystemShutterSound = this.mCamera
                    .enableShutterSound(false);
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
            Log.d(TAG, "onLayout() previewWidth=" + previewWidth + " previewHeight=" + previewHeight + " w=" + width + " h=height");

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
        for (OnCameraPreviewListener onCameraPreviewListener : this.onCameraPreviewListeners) {
            onCameraPreviewListener.onCameraPreviewFrame(data, this.mPreviewFormat);
            mCamera.addCallbackBuffer(mBuffer);
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
            OnCameraPreviewListener onCameraPreviewListener) {
        this.onCameraPreviewListeners.remove(onCameraPreviewListener);
    }

    public void startPreview() {
        Log.d(TAG, "startPreview() previewRunning=" + this.previewRunning
                + ", configured=" + this.configured);
        if (this.mCamera != null && !this.previewRunning && this.configured) {
            this.previewRunning = true;
            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            this.mCamera.startPreview();
            if (this.mAutoFocusManager != null) {
                this.mAutoFocusManager.resume();
            }
            for (OnCameraPreviewListener onCameraPreviewListener : this.onCameraPreviewListeners) {
                onCameraPreviewListener.onCameraPreviewStarted(this.mCamera);
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
            for (OnCameraPreviewListener onCameraPreviewListener : this.onCameraPreviewListeners) {
                onCameraPreviewListener.onCameraPreviewStopped();
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


            int size = mPreviewSize.height * mPreviewSize.width * ImageFormat.getBitsPerPixel(mPreviewFormat) / 8;
            mBuffer = new byte[size];

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