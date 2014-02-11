package org.camdroid.util;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.View;

/**
 * A utility class that helps with showing and hiding system UI such as the
 * status bar and navigation/system bar. This class uses backward-compatibility
 * techniques described in <a href=
 * "http://developer.android.com/training/backward-compatible-ui/index.html">
 * Creating Backward-Compatible UIs</a> to ensure that devices running any
 * version of ndroid OS are supported. More specifically, there are separate
 * implementations of this abstract class: for newer devices,
 * {@link #getInstance} will return a {@link SystemUiHiderHoneycomb} instance,
 * while on older devices {@link #getInstance} will return a
 * {@link SystemUiHiderBase} instance.
 * <p>
 * For more on system bars, see <a href=
 * "http://developer.android.com/design/get-started/ui-overview.html#system-bars"
 * > System Bars</a>.
 * 
 * @see android.view.View#setSystemUiVisibility(int)
 * @see android.view.WindowManager.LayoutParams#FLAG_FULLSCREEN
 */
public abstract class SystemUiHider {
	/**
	 * A callback interface used to listen for system UI visibility changes.
	 */
	public interface OnVisibilityChangeListener {
		/**
		 * Called when the system UI visibility has changed.
		 * 
		 * @param visible
		 *            True if the system UI is visible.
		 */
		public void onVisibilityChange(boolean visible);
	}

	/**
	 * When this flag is set, the
	 * {@link android.view.WindowManager.LayoutParams#FLAG_LAYOUT_IN_SCREEN}
	 * flag will be set on older devices, making the status bar "float" on top
	 * of the activity layout. This is most useful when there are no controls at
	 * the top of the activity layout.
	 * <p>
	 * This flag isn't used on newer devices because the <a
	 * href="http://developer.android.com/design/patterns/actionbar.html">action
	 * bar</a>, the most important structural element of an Android app, should
	 * be visible and not obscured by the system UI.
	 */
	public static final int FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES = 0x1;

	/**
	 * When this flag is set, {@link #show()} and {@link #hide()} will toggle
	 * the visibility of the status bar. If there is a navigation bar, show and
	 * hide will toggle low profile mode.
	 */
	public static final int FLAG_FULLSCREEN = 0x2;

	/**
	 * When this flag is set, {@link #show()} and {@link #hide()} will toggle
	 * the visibility of the navigation bar, if it's present on the device and
	 * the device allows hiding it. In cases where the navigation bar is present
	 * but cannot be hidden, show and hide will toggle low profile mode.
	 */
	public static final int FLAG_HIDE_NAVIGATION = 0x4;

	// public static final int FLAG_IMMERSIVE = 0x8;
	// public static final int FLAG_IMMERSIVE_STICKY = 0x16;

	/**
	 * Creates and returns an instance of {@link SystemUiHider} that is
	 * appropriate for this device. The object will be either a
	 * {@link SystemUiHiderBase} or {@link SystemUiHiderHoneycomb} depending on
	 * the device.
	 * 
	 * @param activity
	 *            The activity whose window's system UI should be controlled by
	 *            this class.
	 * @param anchorView
	 *            The view on which {@link View#setSystemUiVisibility(int)} will
	 *            be called.
	 * @param flags
	 *            Either 0 or any combination of {@link #FLAG_FULLSCREEN},
	 *            {@link #FLAG_HIDE_NAVIGATION}, and
	 *            {@link #FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES}.
	 */
	public static SystemUiHider getInstance(Activity activity, int flags) {
		SystemUiHider systemUiHider;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			systemUiHider = new SystemUiHiderHoneycomb(activity, flags);
		} else {
			systemUiHider = new SystemUiHiderBase(activity, flags);
		}
		systemUiHider.setup();
		return systemUiHider;
	}

	/**
	 * The activity associated with this UI hider object.
	 */
	protected Activity mActivity;

	int mHideDelayMillis = -1;

	/**
	 * The current UI hider flags.
	 * 
	 * @see #FLAG_FULLSCREEN
	 * @see #FLAG_HIDE_NAVIGATION
	 * @see #FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES
	 */
	protected int mFlags;

	/**
	 * The current visibility callback.
	 */
	protected OnVisibilityChangeListener mOnVisibilityChangeListener = new OnVisibilityChangeListener() {
		@Override
		public void onVisibilityChange(boolean visible) {
			if (visible) {
				SystemUiHider.this
						.delayedHide(SystemUiHider.this.mHideDelayMillis);
			}
		}
	};

	protected Handler mHideSystemUiHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			SystemUiHider.this.hide();
		};
	};

	protected SystemUiHider(Activity activity, int flags) {
		this.mActivity = activity;
		this.mFlags = flags;
	}

	public final void delayedHide(int delay) {
		this.mHideSystemUiHandler.removeMessages(0);
		if (delay > 0) {
			this.mHideSystemUiHandler.sendEmptyMessageDelayed(0, delay);
		} else {
			this.hide();
		}
	}

	/**
	 * Hide the system UI.
	 */
	public abstract void hide();

	/**
	 * Returns whether or not the system UI is visible.
	 */
	public abstract boolean isVisible();

	public void setHideDelayMillis(int hideDelayMillis) {
		this.mHideDelayMillis = hideDelayMillis;
	}

	/**
	 * Registers a callback, to be triggered when the system UI visibility
	 * changes.
	 */
	public void setOnVisibilityChangeListener(
			OnVisibilityChangeListener listener) {
		this.mOnVisibilityChangeListener = listener;
	}

	/**
	 * Sets up the system UI hider. Should be called from
	 * {@link Activity#onCreate}.
	 */
	protected abstract void setup();

	/**
	 * Show the system UI.
	 */
	public abstract void show();

	/**
	 * Toggle the visibility of the system UI.
	 */
	public void toggle() {
		if (this.isVisible()) {
			this.hide();
		} else {
			this.show();
		}
	}
}
