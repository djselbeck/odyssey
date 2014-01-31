package org.odyssey.playbackservice;

import java.util.ArrayList;

/**
 * Message object which get passed between PlaybackServiceInterface -> PlaybackServiceHandler
 * @author hendrik
 *
 */
public class ControlObject {
	public static enum PLAYBACK_ACTION {
		ODYSSEY_PLAY, ODYSSEY_PAUSE, ODYSSEY_RESUME, ODYSSEY_TOGGLEPAUSE, ODYSSEY_STOP, ODYSSEY_NEXT, ODYSSEY_PREVIOUS, ODYSSEY_SEEKTO, ODYSSEY_JUMPTO, ODYSSEY_REPEAT, ODYSSEY_RANDOM, ODYSSEY_ENQUEUETRACK, ODYSSEY_ENQUEUETRACKS, ODYSSEY_DEQUEUETRACK, ODYSSEY_DEQUEUETRACKS, ODYSSEY_SETNEXTRACK, ODYSSEY_CLEARPLAYLIST
	}

	private PLAYBACK_ACTION mAction;
	private boolean mBoolparam;
	private int mIntparam;
	private String mStringparam;
	private ArrayList<String> mStringlist = null;

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

	public String getStringParam() {
		return mStringparam;
	}

	public ArrayList<String> getStringList() {
		return mStringlist;
	}

	public int getIntParam() {
		return mIntparam;
	}
}
