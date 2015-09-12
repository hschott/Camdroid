package org.hschott.camdroid;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AutoFocusManager implements Camera.AutoFocusCallback,
        Runnable {

    private class FocusAccelerationEventListener extends
            AccelerationEventListener {
        public static final float ACCELERATION_HYSTERESIS = 0.6f;

        public FocusAccelerationEventListener(Context context) {
            super(context);
        }

        public FocusAccelerationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onAccelerationChanged(double acceleration) {
            if (!AutoFocusManager.this.shouldReFocus) {
                double abs = Math.abs(acceleration);
                AutoFocusManager.this.shouldReFocus = (abs >= ACCELERATION_HYSTERESIS);
            }
        }
    }

    private class FocusAmbientLightEventListener extends
            AmbientLightEventListener {
        private static final float TOO_DARK_LUX = 15.0f;
        private static final float BRIGHT_ENOUGH_LUX = 50.0f;
        private boolean lastValue = false;

        public FocusAmbientLightEventListener(Context context) {
            super(context);
        }

        public FocusAmbientLightEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onAmbientLightChanged(float ambientLightLux) {
            if (ambientLightLux <= TOO_DARK_LUX && !lastValue) {
                lastValue = true;
                CameraManager.setTorch(AutoFocusManager.this.camera, true);
            } else if (ambientLightLux >= BRIGHT_ENOUGH_LUX && lastValue) {
                lastValue = false;
                CameraManager.setTorch(AutoFocusManager.this.camera, false);
            }
        }

    }

    private static final String TAG = AutoFocusManager.class.getSimpleName();

    private static final long AUTO_FOCUS_INTERVAL_MS = 3000L;
    private long autoFocusInterval = AUTO_FOCUS_INTERVAL_MS;

    private final boolean must_call_auto_focus;
    private volatile boolean takePicture;
    private volatile boolean focused;
    private volatile boolean active;
    private volatile boolean shouldReFocus = true;

    private final Camera camera;

    private ScheduledExecutorService executor;

    private final SoundManager soundManager;
    private PictureCallback pictureCallback;
    private ShutterCallback shutterCallback;
    private FocusAccelerationEventListener focusAccelerationEventListener;
    private FocusAmbientLightEventListener focusAmbientLightEventListener;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public AutoFocusManager(final Context context, Camera camera,
                            final boolean canDisableSystemShutterSound) {
        this.camera = camera;
        this.executor = Executors.newSingleThreadScheduledExecutor();

        this.focusAccelerationEventListener = new FocusAccelerationEventListener(
                context, SensorManager.SENSOR_DELAY_GAME);
        this.focusAmbientLightEventListener = new FocusAmbientLightEventListener(
                context, SensorManager.SENSOR_DELAY_GAME);

        String focusMode = this.camera.getParameters().getFocusMode();
        this.must_call_auto_focus = Camera.Parameters.FOCUS_MODE_AUTO
                .equals(focusMode)
                || Camera.Parameters.FOCUS_MODE_MACRO.equals(focusMode);

        int[] soundRes = new int[0];
        if (this.must_call_auto_focus) {
            soundRes = Arrays.copyOf(soundRes, soundRes.length + 1);
            soundRes[soundRes.length - 1] = R.raw.beep;
        }
        if (canDisableSystemShutterSound) {
            soundRes = Arrays.copyOf(soundRes, soundRes.length + 1);
            soundRes[soundRes.length - 1] = R.raw.shutter;
        }

        this.soundManager = new SoundManager(context, soundRes);

        if (canDisableSystemShutterSound) {
            this.shutterCallback = new ShutterCallback() {
                @Override
                public void onShutter() {
                    AutoFocusManager.this.soundManager.play(R.raw.shutter);
                }
            };
        }

    }

    public synchronized void focus() {
        if (this.active && this.must_call_auto_focus) {
            try {
                this.camera.autoFocus(this);
                this.focused = false;
            } catch (RuntimeException re) {
                Log.w(TAG, "Unexpected exception while focusing", re);
            }
        }
    }

    public synchronized boolean isFocused() {
        return this.focused || !this.must_call_auto_focus;
    }

    @Override
    public synchronized void onAutoFocus(boolean success, Camera theCamera) {
        this.focused = success;

        if (this.focused) {
            this.soundManager.play(R.raw.beep);

            if (this.focusAccelerationEventListener.isEnabled()) {
                this.shouldReFocus = false;
            } else {
                this.shouldReFocus = true;
            }
        }

        if (this.takePicture && this.focused) {
            try {
                this.camera.setPreviewCallback(null);
                this.camera.takePicture(this.shutterCallback, null,
                        this.pictureCallback);
                this.takePicture = false;
            } catch (RuntimeException re) {
                Log.w(TAG, "Unexpected exception while delayed takePicture", re);
            }
        } else {
            this.executor.schedule(this, this.autoFocusInterval,
                    TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void pause() {
        this.active = false;
        this.focusAccelerationEventListener.disable();
        this.focusAmbientLightEventListener.disable();
    }

    public synchronized void resume() {
        this.active = true;
        if (this.must_call_auto_focus) {
            this.focusAccelerationEventListener.enable();
            if (this.focusAccelerationEventListener.isEnabled()) {
                this.autoFocusInterval = 750;
            }
        }
        this.focusAmbientLightEventListener.enable();

        this.executor.execute(this);

        Log.d(TAG, "must call auto focus: " + this.must_call_auto_focus);
        Log.d(TAG, "acceleration listener enabled: "
                + this.focusAccelerationEventListener.isEnabled());
        Log.d(TAG, "ambient light listener enabled: "
                + this.focusAmbientLightEventListener.isEnabled());
    }

    @Override
    public synchronized void run() {
        if (this.active && this.must_call_auto_focus) {
            if (this.shouldReFocus) {
                try {
                    this.camera.autoFocus(this);
                    this.focused = false;
                } catch (RuntimeException re) {
                    Log.w(TAG, "Unexpected exception while focusing", re);
                    this.executor.schedule(this, this.autoFocusInterval,
                            TimeUnit.MILLISECONDS);
                }
            } else {
                this.executor.schedule(this, this.autoFocusInterval,
                        TimeUnit.MILLISECONDS);
            }
        }
    }

    public synchronized void stop() {
        this.active = false;
        this.takePicture = false;
        this.focused = false;
        this.pictureCallback = null;

        this.soundManager.release();

        this.executor.shutdownNow();

        this.focusAccelerationEventListener.disable();
        this.focusAmbientLightEventListener.disable();

        if (this.must_call_auto_focus) {
            try {
                this.camera.cancelAutoFocus();
            } catch (RuntimeException re) {
                Log.w(TAG, "Unexpected exception while cancelling focusing", re);
            }
        }

    }

    public synchronized void takePicture(PictureCallback callback) {
        if (this.isFocused()) {
            try {
                this.camera.setPreviewCallback(null);
                this.camera.takePicture(this.shutterCallback, null, callback);
            } catch (RuntimeException re) {
                Log.w(TAG, "Unexpected exception while takePicture", re);
            }
        } else {
            this.pictureCallback = callback;
            this.takePicture = true;
        }
    }
}
