package org.odyssey.playbackservice;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class PlaybackServiceHandler extends Handler {
	private static final String TAG = "OdysseyPlaybackServiceHandler";

	private final WeakReference<PlaybackService> mService;

	public PlaybackServiceHandler(Looper looper, PlaybackService service) {
		super(looper);
		Log.v(TAG, "Handler created");
		mService = new WeakReference<PlaybackService>(service);
	}

	@Override
	public void handleMessage(Message msg) {
		Log.v(TAG, "handleMessage:" + msg);
		super.handleMessage(msg);

		ControlObject msgObj = (ControlObject) msg.obj;

		// Check if object is received
		if (msgObj != null) {
			// Parse message
			if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAY) {
				Log.v(TAG, "Playback requested");
				mService.get().playURI(msgObj.getStringParam());
			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_STOP) {
				mService.get().stop();
			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_PAUSE) {

			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_NEXT) {

			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_PREVIOUS) {

			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_RANDOM) {

			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_REPEAT) {

			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_SEEKTO) {

			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_JUMPTO) {
				mService.get().jumpToIndex(msgObj.getIntParam());
			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACK) {

			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACKS) {

			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACK) {
				mService.get().enqueueTrack(msgObj.getStringParam());
			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACKS) {
				mService.get().enqueueTracks(msgObj.getStringList());
			} else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_SETNEXTRACK) {

			}

		}

	}

}
