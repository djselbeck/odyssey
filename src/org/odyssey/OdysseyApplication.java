package org.odyssey;

import org.odyssey.playbackservice.IOdysseyPlaybackService;

import android.app.Application;

public class OdysseyApplication extends Application {
	private MusicLibraryHelper mLibraryHelper;
	private IOdysseyPlaybackService mPlaybackService;
	
	public void setLibraryHelper(MusicLibraryHelper helper) {
		if (helper != null) {
			mLibraryHelper = helper;
		}
	}
	
	public MusicLibraryHelper getLibraryHelper() {
		return mLibraryHelper;
	}
	
	public void setPlaybackService(IOdysseyPlaybackService service) {
		if (service != null ) {
			mPlaybackService = service;
		}
	}
	
	public IOdysseyPlaybackService getPlaybackService() {
		return mPlaybackService;
	}

}
