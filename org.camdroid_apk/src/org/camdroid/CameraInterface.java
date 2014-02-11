package org.camdroid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class CameraInterface {

	private static final String TAG = CameraInterface.class.getSimpleName();
	public static final int MIN_ALLOWED_HEIGHT = 1536;

	private static Boolean isCameraSupported = null;

	public static final Collection<String> SUPPORTED_FOCUS_MODES = new ArrayList<String>(
			3);

	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			SUPPORTED_FOCUS_MODES
					.add(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		}
		SUPPORTED_FOCUS_MODES.add(Camera.Parameters.FOCUS_MODE_MACRO);
		SUPPORTED_FOCUS_MODES.add(Camera.Parameters.FOCUS_MODE_AUTO);
	}

	protected static Camera.Size calculateCameraPictureSize(
			List<Camera.Size> supportedSizes) {
		Camera.Size ret = null;
		int calcHeight = 0;

		for (Camera.Size size : supportedSizes) {
			int height = size.height;

			if (height < MIN_ALLOWED_HEIGHT) {
				continue;
			} else if (height > calcHeight) {
				ret = size;
				calcHeight = height;
			}
		}

		return ret;
	}

	public static Integer findSettableValue(
			Collection<Integer> supportedValues, Integer... desiredValues) {
		Log.i(TAG, "Supported values: " + supportedValues
				+ " / Desired values: " + Arrays.deepToString(desiredValues));
		Integer result = null;
		if (supportedValues != null) {
			for (Integer desiredValue : desiredValues) {
				if (supportedValues.contains(desiredValue)) {
					result = desiredValue;
					break;
				}
			}
		}
		Log.i(TAG, "Settable value: " + result);
		return result;
	}

	public static String findSettableValue(Collection<String> supportedValues,
			String... desiredValues) {
		Log.i(TAG, "Supported values: " + supportedValues
				+ " / Desired values: " + Arrays.deepToString(desiredValues));
		String result = null;
		if (supportedValues != null) {
			for (String desiredValue : desiredValues) {
				if (supportedValues.contains(desiredValue)) {
					result = desiredValue;
					break;
				}
			}
		}
		Log.i(TAG, "Settable value: " + result);
		return result;
	}

	public static int firstBackFacingCamera() {
		for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
				return i;
		}
		return -1;
	}

	public static boolean isCameraSupported() {
		if (isCameraSupported != null)
			return isCameraSupported;

		Camera camera = openCamera(-1);
		if (camera == null) {
			isCameraSupported = Boolean.FALSE;
		} else {
			Camera.Parameters params = camera.getParameters();

			String focusMode = findSettableValue(
					params.getSupportedFocusModes(),
					SUPPORTED_FOCUS_MODES.toArray(new String[0]));

			Integer previewFormat = findSettableValue(
					params.getSupportedPreviewFormats(), ImageFormat.NV21);

			Integer pictureFormat = findSettableValue(
					params.getSupportedPictureFormats(), ImageFormat.JPEG);

			String whiteBalanceMode = findSettableValue(
					params.getSupportedWhiteBalance(),
					Camera.Parameters.WHITE_BALANCE_AUTO);

			List<Camera.Size> sizes = params.getSupportedPictureSizes();

			Size size = calculateCameraPictureSize(sizes);
			isCameraSupported = Boolean.valueOf(size != null
					&& size.height >= MIN_ALLOWED_HEIGHT && focusMode != null
					&& whiteBalanceMode != null && pictureFormat != null
					&& previewFormat != null);

			camera.release();
		}

		return isCameraSupported;
	}

	public static Camera openCamera(int cameraIndex) {
		Camera camera = null;

		if (cameraIndex == -1) {
			cameraIndex = firstBackFacingCamera();
		}

		Log.d(TAG,
				"Trying to open camera with new open("
						+ Integer.valueOf(cameraIndex) + ")");
		try {
			camera = Camera.open(cameraIndex);
		} catch (RuntimeException e) {
			Log.e(TAG,
					"Camera #" + cameraIndex + " failed to open: "
							+ e.getLocalizedMessage(), e);
		}
		return camera;
	}

	private CameraInterface() {
	}
}
