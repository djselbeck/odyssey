package org.odyssey.playbackservice;

import java.io.IOException;
import java.util.ArrayList;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.PowerManager;
import android.util.Log;

public class GaplessPlayer {
	private final static String TAG = "GaplessPlayer";
	private MediaPlayer mCurrentMediaPlayer = null;
	private boolean mCurrentPrepared = false;
	private boolean mSecondPrepared = false;
	private MediaPlayer mNextMediaPlayer = null;

	private String mPrimarySource = null;
	private String mSecondarySource = null;

	private PlaybackService mPlaybackService;

	public GaplessPlayer(PlaybackService service) {
		this.mTrackFinishedListeners = new ArrayList<GaplessPlayer.OnTrackFinishedListener>();
		this.mTrackStartListeners = new ArrayList<GaplessPlayer.OnTrackStartedListener>();
		mPlaybackService = service;
		Log.v(TAG,"MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());
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
	public void play(String uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		// Another player currently exists try reusing
		if (mCurrentMediaPlayer != null) {
			mCurrentMediaPlayer.reset();
			mCurrentMediaPlayer.release();
		}
		mCurrentMediaPlayer = new MediaPlayer();
		mCurrentPrepared = false;
		mCurrentMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mCurrentMediaPlayer.setDataSource(uri);
		mPrimarySource = uri;
		mCurrentMediaPlayer.setOnCompletionListener(new TrackCompletionListener());
		mCurrentMediaPlayer.setOnPreparedListener(mPrimaryPreparedListener);
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
		} else if (mCurrentMediaPlayer != null && !mCurrentMediaPlayer.isPlaying() && mCurrentPrepared) {
			mCurrentMediaPlayer.start();
			mCurrentMediaPlayer.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
		}

	}
	
	/**
	 * Just pauses currently running player
	 */
	public void pause() {
		if ( mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying() ) {
			mCurrentMediaPlayer.pause();
		}
	}
	
	/** 
	 * Resumes playback
	 */
	public void resume() {
		// FIXME Catch illegal state exception
		if ( mCurrentMediaPlayer != null ) {
			mCurrentMediaPlayer.start();
		}
	}

	/**
	 * Stops mediaplayback
	 */
	public void stop() {
		if (mCurrentMediaPlayer != null && mCurrentPrepared) {
			if(mNextMediaPlayer != null ) {
				mCurrentMediaPlayer.setNextMediaPlayer(null);
				mNextMediaPlayer.reset();
				mNextMediaPlayer.release();
				mNextMediaPlayer = null;
			}
			mCurrentMediaPlayer.reset();
			mCurrentMediaPlayer.release();
			mCurrentMediaPlayer = null;
		}
		mCurrentPrepared = true;
		mSecondPrepared = true;
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
	public void setNextTrack(String uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		mSecondPrepared = false;
		// Next mediaplayer already set, reset
		if (mNextMediaPlayer != null) {
			mCurrentMediaPlayer.setNextMediaPlayer(null);
			mNextMediaPlayer.reset();
			mNextMediaPlayer.release();
		}
		mNextMediaPlayer = new MediaPlayer();
		mNextMediaPlayer.setOnPreparedListener(mSecondaryPreparedListener);
		Log.v(TAG, "Set next track to: " + uri);
		mNextMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mNextMediaPlayer.setDataSource(uri);
		mSecondarySource = uri;
		// Check if primary is prepared before preparing the second one
		if ( mCurrentPrepared ) {
			mNextMediaPlayer.prepareAsync();
		} 
	}

	private OnPreparedListener mPrimaryPreparedListener = new MediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			Log.v(TAG, "Primary MP prepared: " + mp);
			// If mp equals currentMediaPlayback it should start playing
			mCurrentPrepared = true;
			mp.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			mp.start();
			
			// Notify connected listeners
			for (OnTrackStartedListener listener : mTrackStartListeners) {
				listener.onTrackStarted(mPrimarySource);
			}
			if ( mSecondPrepared == false && mNextMediaPlayer != null ) {
				// Delayed initialization second mediaplayer
				mNextMediaPlayer.prepareAsync();
			}
		}
	};

	private OnPreparedListener mSecondaryPreparedListener = new MediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			Log.v(TAG, "Second MP prepared: " + mp);
			
			// If it is nextMediaPlayer it should be set for currentMP
			mp.setWakeMode(mPlaybackService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			mSecondPrepared = true;
			mCurrentMediaPlayer.setNextMediaPlayer(mp);
			Log.v(TAG, "Set Next MP");			
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
	
	public void setVolume(float leftChannel, float rightChannel) {
		if ( mCurrentMediaPlayer != null ) {
			mCurrentMediaPlayer.setVolume(leftChannel, rightChannel);
		}
	}
	
	private class TrackCompletionListener implements MediaPlayer.OnCompletionListener
	{

		@Override
		public void onCompletion(MediaPlayer mp) {
			Log.v(TAG, "Track playback completed");
			// Cleanup old MP
			mp.release();
			mCurrentMediaPlayer = null;
			// Set current MP to next MP
			if (mNextMediaPlayer != null) {
				Log.v(TAG,"set next as current MP");
				mCurrentMediaPlayer = mNextMediaPlayer;
				mCurrentMediaPlayer.setOnCompletionListener(new TrackCompletionListener());
				mPrimarySource = mSecondarySource;
				mSecondarySource = "";
				
				// Notify connected listeners
				for (OnTrackStartedListener listener : mTrackStartListeners) {
					listener.onTrackStarted(mPrimarySource);
				}
				
				mNextMediaPlayer = null;
			} else {
				Log.v(TAG, "Stopping service");
				mPlaybackService.stopService();
			}
			// notify connected services
			for (OnTrackFinishedListener listener : mTrackFinishedListeners) {
				listener.onTrackFinished();
			}
		}
	}

}
