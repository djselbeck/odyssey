package org.odyssey.playbackservice;

import java.io.IOException;
import java.util.ArrayList;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;

public class GaplessPlayer {
	private MediaPlayer currentMediaPlayer = null;
	private boolean currentPrepared = false;
	private MediaPlayer nextMediaPlayer = null;
	
	/**
	 * Initializes the first mediaplayers with uri and prepares it
	 * so it can get started
	 * @param uri - Path to media file
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void play(String uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		// Another player currently exists try reusing
		if ( currentMediaPlayer != null ) {
			currentMediaPlayer.stop();
			currentMediaPlayer.reset();
		} else {
			currentMediaPlayer = new MediaPlayer();
		}
		currentPrepared = false;
		currentMediaPlayer.setDataSource(uri);
		currentMediaPlayer.setOnPreparedListener(mPreparedListener);
		currentMediaPlayer.prepareAsync();
	}
	
	/**
	 * Pauses the currently running mediaplayer
	 * If already paused it continues the playback
	 */
	public void togglePause() {
		// Check if Mediaplayer is running
		if ( currentMediaPlayer != null && 
				currentMediaPlayer.isPlaying() ) {
			currentMediaPlayer.pause();
		}
		else if ( currentMediaPlayer != null && 
			!currentMediaPlayer.isPlaying() && currentPrepared ) {
				currentMediaPlayer.start();
			}
			
	}
	
	/**
	 * Stops mediaplayback
	 */
	public void stop() {
		if( currentMediaPlayer != null && 
				currentPrepared) {
			currentMediaPlayer.stop();			
		}
	}
	
	/**
	 * Sets next mediaplayer to uri and start preparing it.
	 * if next mediaplayer was already initialized it gets resetted
	 * @param uri
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void setNextTrack(String uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		// Next mediaplayer already set, reset
		if ( nextMediaPlayer != null ) {
			nextMediaPlayer.reset();
		}
		else {
			nextMediaPlayer = new MediaPlayer();
		}
		nextMediaPlayer.setDataSource(uri);
		nextMediaPlayer.setOnPreparedListener(mPreparedListener);
	}
	
	private OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
		
		@Override
		public void onPrepared(MediaPlayer mp) {
			// If mp equals currentMediaPlayback it should start playing
			if ( mp.equals(currentMediaPlayer) ) {
				currentPrepared = true;
				mp.start();
			} // If it is nextMediaPlayer it should be set for currentMP
			else if ( mp.equals(nextMediaPlayer) ) {
				currentMediaPlayer.setNextMediaPlayer(mp);
			}
		}
	};
	
	private OnCompletionListener mCompletionListener = new OnCompletionListener() {
		
		@Override
		public void onCompletion(MediaPlayer mp) {
			// Cleanup old MP
			currentMediaPlayer.release();
			currentMediaPlayer = null;
			// Set current MP to next MP
			if ( nextMediaPlayer != null) {
				currentMediaPlayer = nextMediaPlayer;
				nextMediaPlayer = null;
			}
			// notify connected services
			for ( OnTrackFinishedListener listener : mTrackFinishedListeners) {
				listener.onTrackFinished();
			}
		}
	};
	
	
	// Notification for Services using GaplessPlayer
	public interface OnTrackFinishedListener {
		void onTrackFinished();
	}
	
	private ArrayList<OnTrackFinishedListener> mTrackFinishedListeners;
	
	public void setOnTrackFinishedListener(OnTrackFinishedListener listener) {
		mTrackFinishedListeners.add(listener);
	}
	
	public void removeOnTrackFinishedListener(OnTrackFinishedListener listener) {
		mTrackFinishedListeners.remove(listener);
	}
	

}
