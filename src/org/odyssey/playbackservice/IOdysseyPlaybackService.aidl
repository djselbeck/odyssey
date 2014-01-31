package org.odyssey.playbackservice;

import org.odyssey.IOdysseyNowPlayingCallback;

interface IOdysseyPlaybackService {
	
	// Controls the player with predefined actions
	void play(String uri);
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
	
	void enqueueTrack(String track);
	void enqueueTracks(in List<String> tracks);
	void dequeueTrack(String track);
	void dequeueTracks(in List<String> tracks);
	void clearPlaylist();
	
	List<String> getCurrentList();
	
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
