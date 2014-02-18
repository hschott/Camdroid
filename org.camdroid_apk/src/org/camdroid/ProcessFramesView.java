package org.camdroid;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.camdroid.OnCameraPreviewListener.AutoFocusManagerAware;
import org.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.camdroid.processor.FrameProcessor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class ProcessFramesView extends ViewGroup implements
		SurfaceHolder.Callback, OnCameraPreviewListener, AutoFocusManagerAware,
		FrameDrawer {

	private static final String TAG = ProcessFramesView.class.getSimpleName();

	private SurfaceHolder mHolder;
	private AutoFocusManager mAutoFocusManager;
	private ExecutorService mExecutor;
	private FrameProcessor mProcessor;
	private Camera.Size mPreviewSize;
	private float mScale;

	private FpsMeter fpsMeter;
	private Paint fpsPaint;

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
	public void drawBitmap(Bitmap bitmap) {
		if (this.mHolder == null)
			return;

		Canvas canvas = null;
		try {
			synchronized (this.mHolder) {
				canvas = this.mHolder.lockCanvas(null);

				int bmpWidth = bitmap.getWidth();
				int bmpHeight = bitmap.getHeight();

				int canvasWidth = canvas.getWidth();
				int canvasHeight = canvas.getHeight();

				if (this.mScale != 0) {
					canvas.drawBitmap(bitmap, new Rect(0, 0, bmpWidth,
							bmpHeight),
							new Rect((int) ((canvasWidth - this.mScale
									* bmpWidth) / 2),
									(int) ((canvasHeight - this.mScale
											* bmpHeight) / 2),
									(int) ((canvasWidth - this.mScale
											* bmpWidth) / 2 + this.mScale
											* bmpWidth),
									(int) ((canvasHeight - this.mScale
											* bmpHeight) / 2 + this.mScale
											* bmpHeight)), null);
				} else {
					canvas.drawBitmap(bitmap, new Rect(0, 0, bmpWidth,
							bmpHeight), new Rect((canvasWidth - bmpWidth) / 2,
							(canvasHeight - bmpHeight) / 2,
							(canvasWidth - bmpWidth) / 2 + bmpWidth,
							(canvasHeight - bmpHeight) / 2 + bmpHeight), null);
				}

				String fps = this.fpsMeter.measure();
				canvas.drawText(fps, 12, canvasHeight - 12, this.fpsPaint);
			}
		} finally {
			if (canvas != null) {
				this.mHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	@Override
	public AutoFocusManager getAutoFocusManager() {
		return this.mAutoFocusManager;
	}

	public FrameProcessor getFrameProcessor() {
		return this.mProcessor;
	}

	private void initView(Context context) {
		Log.d(TAG, "initView()");

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
			return;

		if (this.mProcessor == null)
			return;

		if (this.mProcessor.put(data)) {
			this.mExecutor.execute(this.mProcessor);
		}
	}

	@Override
	public void onCameraPreviewStarted(Camera camera) {
		this.mPreviewSize = camera.getParameters().getPreviewSize();

		if (this.mProcessor != null) {
			this.mProcessor.allocate(this.mPreviewSize.width,
					this.mPreviewSize.height);
		}

		this.mScale = Math.min(((float) this.getMeasuredHeight())
				/ this.mPreviewSize.height, ((float) this.getMeasuredWidth())
				/ this.mPreviewSize.width);

		this.mExecutor = Executors.newFixedThreadPool(1);

		this.fpsMeter = new FpsMeter(this.mPreviewSize.width,
				this.mPreviewSize.height);

		this.fpsPaint = new Paint();
		this.fpsPaint.setColor(this.getResources().getColor(
				R.color.config_ui_green));
		this.fpsPaint.setTextSize(20);
	}

	@Override
	public void onCameraPreviewStopped() {
		this.mExecutor.shutdownNow();
		if (this.mProcessor != null) {
			this.mProcessor.release();
		}
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

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int width = resolveSize(this.getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(this.getSuggestedMinimumHeight(),
				heightMeasureSpec);
		this.setMeasuredDimension(width, height);
	}

	@Override
	public void setAutoFocusManager(AutoFocusManager autoFocusManager) {
		this.mAutoFocusManager = autoFocusManager;
	}

	public void setFrameProcessor(FrameProcessor processor) {
		if (processor == null) {
			FrameProcessor tmp = this.mProcessor;
			this.mProcessor = null;
			if (tmp != null) {
				tmp.release();
			}
			return;
		}

		if (this.mProcessor != null
				&& processor.getClass().isAssignableFrom(
						this.mProcessor.getClass()))
			return;

		if (this.mProcessor != null) {
			FrameProcessor tmp = this.mProcessor;
			this.mProcessor = processor;
			tmp.release();
		} else {
			this.mProcessor = processor;
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		this.mHolder = holder;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		this.mHolder = null;
	}
}
