package org.odyssey.playbackservice;

import org.odyssey.IOdysseyNowPlayingCallback;
import org.odyssey.playbackservice.TrackItem;

interface IOdysseyPlaybackService {
	
	// Controls the player with predefined actions
	void play(in TrackItem track);
	void pause();
	void resume();
	void stop();
	void next();
	void previous();
	void togglePause();
	
	/**
	 * position = position in current track ( in seconds)
	 */
	void seekTo(int position);
	/** 
	 * position = playlist position of jump target
	 */
	void jumpTo(int position);
	
	void setRandom(boolean random);
	void setRepeat(boolean repeat);
	
	// track is the full uri with "file://" !
	void setNextTrack(String track);
	
	void enqueueTrack(in TrackItem track);
	void enqueueTracks(in List<TrackItem> tracks);
	void dequeueTrack(in TrackItem track);
	void dequeueTracks(in List<TrackItem> tracks);
	void clearPlaylist();
	
	void getCurrentList(out List<TrackItem> tracks);
	
	// Information getters
	String getArtist();
	String getAlbum();
	String getTrackname();
	int getTrackNo();
	int getBitrate();
	int getSamplerate();
	
	
	void registerNowPlayingReceiver(IOdysseyNowPlayingCallback receiver);
	void unregisterNowPlayingReceiver(IOdysseyNowPlayingCallback receiver);
}
