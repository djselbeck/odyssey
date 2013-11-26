package org.odyssey.playbackservice;

import java.util.ArrayList;

public class ControlObject {
	public static enum PLAYBACK_ACTION {
		ODYSSEY_PLAY, ODYSSEY_PAUSE, ODYSSEY_STOP, ODYSSEY_NEXT, ODYSSEY_PREVIOUS,
		ODYSSEY_SEEKTO, ODYSSEY_JUMPTO, ODYSSEY_REPEAT, ODYSSEY_RANDOM, ODYSSEY_ENQUEUETRACK,
		ODYSSEY_ENQUEUETRACKS, ODYSSEY_DEQUEUETRACK, ODYSSEY_DEQUEUETRACKS,
		ODYSSEY_SETNEXTRACK
	}
	
	private PLAYBACK_ACTION mAction;
	boolean mBoolparam;
	int mIntparam;
	String mStringparam;
	ArrayList<String> mStringlist = null;
	
	public ControlObject(PLAYBACK_ACTION action) {
		mAction = action;
	}
	
	public ControlObject(PLAYBACK_ACTION action, boolean param) {
		mBoolparam = param;
		mAction = action;
	}
	
	public ControlObject(PLAYBACK_ACTION action, int param) {
		mIntparam = param;
		mAction = action;
	}
	
	public ControlObject(PLAYBACK_ACTION action, String param) {
		mStringparam = param;
		mAction = action;
	}
	
	public ControlObject(PLAYBACK_ACTION action, ArrayList<String> list) {
		mStringlist = list;
		mAction = action;
	}
	
	public PLAYBACK_ACTION getAction() {
		return mAction;
	}
}
