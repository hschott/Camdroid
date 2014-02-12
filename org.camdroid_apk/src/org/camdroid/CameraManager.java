package org.camdroid;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;

public class CameraManager {
	static class SizeComparator implements Comparator<Size> {
		@Override
		public int compare(Size a, Size b) {
			double aPixels = a.height * a.width;
			double bPixels = b.height * b.width;
			if (bPixels < aPixels)
				return -1;
			if (bPixels > aPixels)
				return 1;
			return 0;
		}
	}

	private static final String TAG = CameraManager.class.getSimpleName();

	static Size getOptimalPictureSize(List<Size> sizes, int minHeight) {

		// Sort by size, descending
		Collections.sort(sizes, new SizeComparator());

		Size fallback = sizes.get(0);

		// Remove sizes that are unsuitable
		Iterator<Size> it = sizes.iterator();
		while (it.hasNext()) {
			Size size = it.next();
			double realWidth = size.width;
			double realHeight = size.height;

			boolean isCandidatePortrait = realWidth < realHeight;
			double maybeFlippedHeight = isCandidatePortrait ? realWidth
					: realHeight;

			if (maybeFlippedHeight == minHeight) {
				Log.i(TAG, "Using optimal picture size: " + size.width + "x"
						+ size.height);
				return size;
			}

			if (maybeFlippedHeight < minHeight) {
				it.remove();
				continue;
			}

		}

		if (!sizes.isEmpty()) {
			Size size = sizes.get(sizes.size() - 1);
			Log.i(TAG, "Using suitable picture size: " + size.width + "x"
					+ size.height);
			return size;
		}

		// If there is nothing at all suitable, return full picture size
		Log.i(TAG, "No suitable picture sizes, using fallback: "
				+ fallback.width + "x" + fallback.height);
		return fallback;
	}

	static Size getOptimalPreviewSize(List<Size> sizes, double targetRatio,
			int w, int h) {

		// Sort by size, descending
		Collections.sort(sizes, new SizeComparator());

		final double ASPECT_TOLERANCE = 0;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int target = w;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double realWidth = size.width;
			double realHeight = size.height;

			boolean isCandidatePortrait = realWidth < realHeight;
			double maybeFlippedWidth = isCandidatePortrait ? realHeight
					: realWidth;
			double maybeFlippedHeight = isCandidatePortrait ? realWidth
					: realHeight;
			double ratio = maybeFlippedWidth / maybeFlippedHeight;

			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
				continue;
			}
			if (Math.abs(maybeFlippedWidth - target) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(maybeFlippedWidth - target);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				double realWidth = size.width;
				double realHeight = size.height;

				boolean isCandidatePortrait = realWidth < realHeight;
				double maybeFlippedWidth = isCandidatePortrait ? realHeight
						: realWidth;

				if (Math.abs(maybeFlippedWidth - target) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(maybeFlippedWidth - target);
				}
			}
			Log.i(TAG, "Using suitable preview size: " + optimalSize.width
					+ "x" + optimalSize.height);
		} else {
			Log.i(TAG, "Using optimal preview size: " + optimalSize.width + "x"
					+ optimalSize.height);
		}

		return optimalSize;
	}

	private static void initFlashMode(Camera.Parameters params) {
		String flashMode = null;
		flashMode = CameraInterface.findSettableValue(
				params.getSupportedFlashModes(),
				Camera.Parameters.FLASH_MODE_OFF);
		if (flashMode != null) {
			params.setFlashMode(flashMode);
		}
	}

	private static void initFocusMode(Camera.Parameters params) {
		String focusMode = null;
		focusMode = CameraInterface.findSettableValue(
				params.getSupportedFocusModes(),
				CameraInterface.SUPPORTED_FOCUS_MODES.toArray(new String[0]));
		if (focusMode != null) {
			params.setFocusMode(focusMode);
		}
	}

	static void initializeCamera(Camera camera) {

		Camera.Parameters params = camera.getParameters();

		initSceneMode(params);

		initFocusMode(params);

		initWhiteBalanceMode(params);

		initFlashMode(params);

		initPictureFormat(params);

		initPictureSize(params);

		initPreviewFormat(params);

		camera.setParameters(params);
		Log.d(TAG, camera.getParameters().flatten());
	}

	private static void initPictureFormat(Camera.Parameters params) {
		Integer pictureFormat = null;
		pictureFormat = CameraInterface.findSettableValue(
				params.getSupportedPictureFormats(), ImageFormat.JPEG);
		if (pictureFormat != null) {
			params.setPictureFormat(pictureFormat);
			params.setJpegQuality(100);
		}

		if (params.isZoomSupported()) {
			params.setZoom(0);
		}

	}

	private static void initPictureSize(Camera.Parameters params) {
		List<Camera.Size> supportedPictureSizes = params
				.getSupportedPictureSizes();
		Camera.Size pictureSize = CameraManager.getOptimalPictureSize(
				supportedPictureSizes, CameraInterface.MIN_ALLOWED_HEIGHT);
		params.setPictureSize(pictureSize.width, pictureSize.height);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
	private static void initPreviewFormat(Camera.Parameters params) {
		Integer previewFormat = null;
		previewFormat = CameraInterface.findSettableValue(
				params.getSupportedPreviewFormats(), ImageFormat.NV21);
		if (previewFormat != null) {
			params.setPreviewFormat(previewFormat);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			if (params.isVideoStabilizationSupported()) {
				params.setVideoStabilization(true);
			}
		}
	}

	static void initPreviewSize(Camera.Parameters params,
			Camera.Size pictureSize, int w, int h) {
		List<Camera.Size> supportedPreviewSizes = params
				.getSupportedPreviewSizes();
		Camera.Size previewSize = CameraManager.getOptimalPreviewSize(
				supportedPreviewSizes, (double) pictureSize.width
						/ (double) pictureSize.height, w, h);
		params.setPreviewSize(previewSize.width, previewSize.height);
	}

	private static void initSceneMode(Camera.Parameters params) {
		String sceneMode = null;
		sceneMode = CameraInterface.findSettableValue(
				params.getSupportedSceneModes(),
				Camera.Parameters.SCENE_MODE_AUTO);
		if (sceneMode != null) {
			params.setSceneMode(sceneMode);
		}
	}

	private static void initWhiteBalanceMode(Camera.Parameters params) {
		String whiteBalanceMode = null;
		whiteBalanceMode = CameraInterface.findSettableValue(
				params.getSupportedWhiteBalance(),
				Camera.Parameters.WHITE_BALANCE_AUTO);
		if (whiteBalanceMode != null) {
			params.setWhiteBalance(whiteBalanceMode);
		}
	}

	private static void setFlashMode(Camera.Parameters parameters,
			boolean enabled) {
		String flashMode;
		if (enabled) {
			flashMode = CameraInterface.findSettableValue(
					parameters.getSupportedFlashModes(),
					Camera.Parameters.FLASH_MODE_TORCH,
					Camera.Parameters.FLASH_MODE_ON);
		} else {
			flashMode = CameraInterface.findSettableValue(
					parameters.getSupportedFlashModes(),
					Camera.Parameters.FLASH_MODE_OFF);
		}
		if (flashMode != null) {
			parameters.setFlashMode(flashMode);
		}
	}

	public static void setTorch(Camera camera, boolean enabled) {
		if (camera == null)
			return;
		Camera.Parameters parameters = camera.getParameters();
		setFlashMode(parameters, enabled);
		camera.setParameters(parameters);
	}

	private CameraManager() {
	}
}
