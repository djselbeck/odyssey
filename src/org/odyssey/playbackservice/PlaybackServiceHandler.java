package org.odyssey.playbackservice;

import java.lang.ref.WeakReference;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class PlaybackServiceHandler extends Handler {
	private static final String TAG = "OdysseyPlaybackServiceHandler"; 
	
	private final WeakReference<PlaybackService> mService;
	
	public PlaybackServiceHandler(Looper looper, PlaybackService service) {
		super(looper);
		Log.v(TAG,"Handler created");
		mService = new WeakReference<PlaybackService>(service);
	}
	
	@Override
	public void handleMessage(Message msg) {
		Log.v(TAG, "handleMessage:" + msg);
		super.handleMessage(msg);
		if ( ((String)msg.obj).equals(PlaybackService.ACTION_TESTPLAY)) {
			Log.v(TAG, PlaybackService.ACTION_TESTPLAY );
			mService.get().startTestPlayback();
		}
	}
	
	
	
}
