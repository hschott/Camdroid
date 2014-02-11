package org.camdroid;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.camdroid.CameraPreviewView.OnCameraPreviewListener;
import org.camdroid.CameraPreviewView.OnCameraPreviewListener.AutoFocusManagerAware;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class ProcessFramesView extends ViewGroup implements
		SurfaceHolder.Callback, OnCameraPreviewListener, AutoFocusManagerAware {

	static {
		OpenCVLoader.init();
	}

	private class FrameProcessor implements Runnable {
		volatile boolean dataLock = false;

		Mat data;
		Mat rgba;

		public FrameProcessor(Context context) {
			rgba = new Mat();
		}

		@Override
		public void run() {
			if (this.dataLock)
				return;

			this.dataLock = true;

			try {
				Mat gray = this.data.submat(0, mPreviewSize.height, 0,
						mPreviewSize.width).clone();

				Imgproc.adaptiveThreshold(gray, gray, 255,
						Imgproc.THRESH_BINARY_INV,
						Imgproc.ADAPTIVE_THRESH_MEAN_C, 5, 15);

				Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2BGR, 0);
				drawToSurface(rgba);
			} catch (Exception e) {
			}

			this.dataLock = false;
		}
	}

	private static final String TAG = ProcessFramesView.class.getSimpleName();

	private SurfaceHolder mHolder;
	private AutoFocusManager mAutoFocusManager;
	private ExecutorService mExecutor;
	private FrameProcessor mProcessor;
	private Camera.Size mPreviewSize;
	private Bitmap mCacheBitmap;

	private float mScale;

	public ProcessFramesView(Context context) {
		super(context);
		this.initView(context);
	}

	public ProcessFramesView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.initView(context);
	}

	public ProcessFramesView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.initView(context);
	}

	@Override
	public AutoFocusManager getAutoFocusManager() {
		return this.mAutoFocusManager;
	}

	private void initView(Context context) {
		Log.d(TAG, "initView()");

		if (!this.isInEditMode())
			this.mProcessor = new FrameProcessor(this.getContext()
					.getApplicationContext());

		this.setBackgroundColor(this.getResources().getColor(
				android.R.color.black));

		SurfaceView view = new SurfaceView(context);
		view.setWillNotDraw(false);

		this.addView(view, 0);

		SurfaceHolder holder = view.getHolder();
		holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		holder.addCallback(this);
	}

	@Override
	public void onCameraPreviewFrame(byte[] data, int previewFormat) {
		if (ImageFormat.NV21 != previewFormat)
			return;

		if (!this.isEnabled())
			return;

		if (this.mAutoFocusManager != null
				&& !this.mAutoFocusManager.isFocused())
			// return;

			if (this.mProcessor.dataLock)
				return;

		this.mProcessor.dataLock = true;
		this.mProcessor.data.put(0, 0, data);
		this.mExecutor.execute(this.mProcessor);
		this.mProcessor.dataLock = false;
	}

	@Override
	public void onCameraPreviewStarted(Camera camera) {
		mPreviewSize = camera.getParameters().getPreviewSize();

		this.mProcessor.data = new Mat(this.mPreviewSize.height
				+ (this.mPreviewSize.height / 2), this.mPreviewSize.width,
				CvType.CV_8UC1);

		mCacheBitmap = Bitmap.createBitmap(mPreviewSize.width,
				mPreviewSize.height, Bitmap.Config.ARGB_8888);

		mScale = Math.min(((float) getMeasuredHeight()) / mPreviewSize.height,
				((float) getMeasuredWidth()) / mPreviewSize.width);

		this.mExecutor = Executors.newFixedThreadPool(1);
	}

	private void drawToSurface(Mat rgba) {
		if (mHolder == null) {
			return;
		}

		Canvas canvas = null;
		try {
			synchronized (mHolder) {
				canvas = mHolder.lockCanvas(null);

				Utils.matToBitmap(rgba, mCacheBitmap);

				if (mScale != 0) {
					canvas.drawBitmap(
							mCacheBitmap,
							new Rect(0, 0, mCacheBitmap.getWidth(),
									mCacheBitmap.getHeight()),
							new Rect(
									(int) ((canvas.getWidth() - mScale
											* mCacheBitmap.getWidth()) / 2),
									(int) ((canvas.getHeight() - mScale
											* mCacheBitmap.getHeight()) / 2),
									(int) ((canvas.getWidth() - mScale
											* mCacheBitmap.getWidth()) / 2 + mScale
											* mCacheBitmap.getWidth()),
									(int) ((canvas.getHeight() - mScale
											* mCacheBitmap.getHeight()) / 2 + mScale
											* mCacheBitmap.getHeight())), null);
				} else {
					canvas.drawBitmap(
							mCacheBitmap,
							new Rect(0, 0, mCacheBitmap.getWidth(),
									mCacheBitmap.getHeight()),
							new Rect((canvas.getWidth() - mCacheBitmap
									.getWidth()) / 2,
									(canvas.getHeight() - mCacheBitmap
											.getHeight()) / 2, (canvas
											.getWidth() - mCacheBitmap
											.getWidth())
											/ 2 + mCacheBitmap.getWidth(),
									(canvas.getHeight() - mCacheBitmap
											.getHeight())
											/ 2
											+ mCacheBitmap.getHeight()), null);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (canvas != null) {
				mHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	@Override
	public void onCameraPreviewStopped() {
		this.mExecutor.shutdownNow();
		this.mProcessor.data.release();
		this.mCacheBitmap.recycle();
	}

	@Override
	public void setAutoFocusManager(AutoFocusManager autoFocusManager) {
		this.mAutoFocusManager = autoFocusManager;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mHolder = holder;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mHolder = null;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int width = resolveSize(this.getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(this.getSuggestedMinimumHeight(),
				heightMeasureSpec);
		this.setMeasuredDimension(width, height);
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

}
