package org.hschott.camdroid;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;

public final class SoundManager {
	private static final String TAG = SoundManager.class.getSimpleName();

	private SparseArray<MediaPlayer> sounds = new SparseArray<MediaPlayer>();

	private AudioManager audioManager;

	public SoundManager(Context context, int... resIds) {

		if (resIds.length > 0) {
			Intent i = new Intent("com.android.music.musicservicecommand");
			i.putExtra("command", "pause");
			context.sendBroadcast(i);

			this.audioManager = (AudioManager) context
					.getSystemService(Context.AUDIO_SERVICE);
		}

		for (final int resId : resIds) {
			MediaPlayer mediaPlayer = new MediaPlayer();

			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			AssetFileDescriptor file = context.getResources()
					.openRawResourceFd(resId);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(),
						file.getStartOffset(), file.getLength());
				file.close();
			} catch (IOException e) {
			}

			mediaPlayer.setOnErrorListener(new OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					Log.e(TAG, "mp: " + mp.getAudioSessionId()
							+ " error for resId: " + resId + " error: " + what
							+ " / " + extra);
					return false;
				}
			});

			mediaPlayer.setOnInfoListener(new OnInfoListener() {
				@Override
				public boolean onInfo(MediaPlayer mp, int what, int extra) {
					Log.i(TAG, "mp: " + mp.getAudioSessionId()
							+ " info for resId: " + resId + " error: " + what
							+ " / " + extra);
					return false;
				}
			});

			mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					Log.d(TAG, "mp: " + mp.getAudioSessionId()
							+ " prepared for resId: " + resId);
					SoundManager.this.sounds.put(resId, mp);
				}
			});
			mediaPlayer.prepareAsync();
		}
	};

	private float currentVolume() {
		float actualVolume = this.audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = this.audioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume = actualVolume / maxVolume;
		return volume;
	}

	public void play(int resId) {
		final MediaPlayer mp = this.sounds.get(resId);
		if (mp != null) {
			float volume = this.currentVolume();

			synchronized (mp) {
				mp.setVolume(volume, volume);
				Log.d(TAG, "mp: " + mp.getAudioSessionId() + " play resId: "
						+ resId);
				mp.start();
			}
		}
	}

	public void release() {
		for (int i = 0; i < this.sounds.size(); i++) {
			MediaPlayer mp = this.sounds.valueAt(i);
			Log.d(TAG, "mp: " + mp.getAudioSessionId() + " release");
			mp.release();
		}
		this.sounds.clear();
	}

	public void stop(int resId) {
		final MediaPlayer mp = this.sounds.get(resId);
		this.stop(mp);
	}

	private void stop(final MediaPlayer mp) {
		if (mp == null)
			return;

		final Handler handler = new Handler();
		final float currentVolume = SoundManager.this.currentVolume();

		synchronized (mp) {
			Runnable mute = new Runnable() {
				float volume = currentVolume;

				@Override
				public void run() {
					boolean playing = false;
					try {
						playing = mp.isPlaying();
					} catch (IllegalStateException e) {
						return;
					}
					if (playing) {
						if (this.volume > 0.0f) {
							this.volume -= .25f;
							Log.d(TAG, "mp: " + mp.getAudioSessionId()
									+ " mute to: " + this.volume);
							mp.setVolume(this.volume, this.volume);
							handler.postDelayed(this, 250);
						} else {
							Log.d(TAG, "mp: " + mp.getAudioSessionId()
									+ " pause");
							mp.pause();
							mp.seekTo(0);
						}
					}
				}
			};

			handler.post(mute);
		}
	}
}
