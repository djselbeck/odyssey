package org.odyssey.playbackservice;

import java.io.IOException;
import java.util.ArrayList;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;

public class GaplessPlayer {
	private MediaPlayer mCurrentMediaPlayer = null;
	private boolean mCurrentPrepared = false;
	private MediaPlayer mNextMediaPlayer = null;

	private String mPrimarySource;
	private String mSecondarySource;

	public GaplessPlayer() {
		this.mTrackFinishedListeners = new ArrayList<GaplessPlayer.OnTrackFinishedListener>();
		this.mTrackStartListeners = new ArrayList<GaplessPlayer.OnTrackStartedListener>();
	}

	/**
	 * Initializes the first mediaplayers with uri and prepares it so it can get
	 * started
	 * 
	 * @param uri
	 *            - Path to media file
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void play(String uri) throws IllegalArgumentException,
			SecurityException, IllegalStateException, IOException {
		// Another player currently exists try reusing
		if (mCurrentMediaPlayer != null) {
			mCurrentMediaPlayer.stop();
			mCurrentMediaPlayer.reset();
		} else {
			mCurrentMediaPlayer = new MediaPlayer();
		}
		mCurrentPrepared = false;
		mCurrentMediaPlayer.setDataSource(uri);
		mPrimarySource = uri;
		mCurrentMediaPlayer.setOnPreparedListener(mPreparedListener);
		mCurrentMediaPlayer.prepareAsync();
	}

	/**
	 * Pauses the currently running mediaplayer If already paused it continues
	 * the playback
	 */
	public void togglePause() {
		// Check if Mediaplayer is running
		if (mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying()) {
			mCurrentMediaPlayer.pause();
		} else if (mCurrentMediaPlayer != null
				&& !mCurrentMediaPlayer.isPlaying() && mCurrentPrepared) {
			mCurrentMediaPlayer.start();
		}

	}

	/**
	 * Stops mediaplayback
	 */
	public void stop() {
		if (mCurrentMediaPlayer != null && mCurrentPrepared) {
			mCurrentMediaPlayer.stop();
		}
	}

	/**
	 * Sets next mediaplayer to uri and start preparing it. if next mediaplayer
	 * was already initialized it gets resetted
	 * 
	 * @param uri
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void setNextTrack(String uri) throws IllegalArgumentException,
			SecurityException, IllegalStateException, IOException {
		// Next mediaplayer already set, reset
		if (mNextMediaPlayer != null) {
			mNextMediaPlayer.reset();
		} else {
			mNextMediaPlayer = new MediaPlayer();
		}
		mNextMediaPlayer.setDataSource(uri);
		mSecondarySource = uri;
		mNextMediaPlayer.setOnPreparedListener(mPreparedListener);
	}

	private OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			// If mp equals currentMediaPlayback it should start playing
			if (mp.equals(mCurrentMediaPlayer)) {
				mCurrentPrepared = true;
				mp.start();
				// Notify connected listeners
				for (OnTrackStartedListener listener : mTrackStartListeners) {
					listener.onTrackStarted(mPrimarySource);
				}
			} // If it is nextMediaPlayer it should be set for currentMP
			else if (mp.equals(mNextMediaPlayer)) {
				mCurrentMediaPlayer.setNextMediaPlayer(mp);
			}
		}
	};

	private OnCompletionListener mCompletionListener = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			// Cleanup old MP
			mCurrentMediaPlayer.release();
			mCurrentMediaPlayer = null;
			// Set current MP to next MP
			if (mNextMediaPlayer != null) {
				mCurrentMediaPlayer = mNextMediaPlayer;
				mNextMediaPlayer = null;
			}
			// notify connected services
			for (OnTrackFinishedListener listener : mTrackFinishedListeners) {
				listener.onTrackFinished();
			}
		}
	};

	// Notification for Services using GaplessPlayer
	public interface OnTrackFinishedListener {
		void onTrackFinished();
	}

	public interface OnTrackStartedListener {
		void onTrackStarted(String URI);
	}

	// Track finish notification
	private ArrayList<OnTrackFinishedListener> mTrackFinishedListeners;

	public void setOnTrackFinishedListener(OnTrackFinishedListener listener) {
		mTrackFinishedListeners.add(listener);
	}

	public void removeOnTrackFinishedListener(OnTrackFinishedListener listener) {
		mTrackFinishedListeners.remove(listener);
	}

	// Track start notification
	private ArrayList<OnTrackStartedListener> mTrackStartListeners;

	public void setOnTrackStartListener(OnTrackStartedListener listener) {
		mTrackStartListeners.add(listener);
	}

	public void removeOnTrackStartListener(OnTrackStartedListener listener) {
		mTrackStartListeners.remove(listener);
	}

	public boolean isRunning() {
		if (mCurrentMediaPlayer != null) {
			return mCurrentMediaPlayer.isPlaying();
		}
		return false;
	}

}
