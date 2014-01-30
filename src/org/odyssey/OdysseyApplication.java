package org.odyssey;

import java.lang.ref.WeakReference;

import org.odyssey.playbackservice.IOdysseyPlaybackService;
import org.odyssey.playbackservice.PlaybackService;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class OdysseyApplication extends Application {
	private MusicLibraryHelper mLibraryHelper;
	private IOdysseyPlaybackService mPlaybackService = null;
	private PlaybackServiceConnection mPBServiceConnection = null;
	private IOdysseyNowPlayingCallback mPBCallback = null;
	
	private static final String TAG = "OdysseyApplication";
	
	public void setLibraryHelper(MusicLibraryHelper helper) {
		if (helper != null) {
			mLibraryHelper = helper;
		}
	}
	
	public MusicLibraryHelper getLibraryHelper() {
		return mLibraryHelper;
	}
	
	private void setPlaybackService(IOdysseyPlaybackService service) {
		if (service != null ) {
			mPlaybackService = service;
		}
	}
	
	public IOdysseyPlaybackService getPlaybackService() {
		if ( mPlaybackService == null ) {
			// Create initial service connection here
	        // create service connection
			mPBServiceConnection = new PlaybackServiceConnection(this);
	        Intent serviceStartIntent = new Intent(this,PlaybackService.class);
	        startService(serviceStartIntent);
	        bindService(new Intent(IOdysseyPlaybackService.class.getName()), 
	        		mPBServiceConnection, Context.BIND_AUTO_CREATE);
		}
		return mPlaybackService;
	}	

	
	private class PlaybackServiceConnection implements ServiceConnection {
		
		private OdysseyApplication mApplication;
		
		public PlaybackServiceConnection(OdysseyApplication application ) {
			mApplication = application;
		}
		
    	@Override
    	public void onServiceConnected(ComponentName name, IBinder service) {
    		Log.v(TAG,"Service connection created");
    		if ( mPlaybackService == null ) { 
    			setPlaybackService(IOdysseyPlaybackService.Stub.asInterface(service));
    		}
    		// Create callback connection
    		try {
    			if ( mPBCallback == null) {
    				mPBCallback = new OdysseyNowPlayingReceiver(mApplication);
    			}
    			mPlaybackService.registerNowPlayingReceiver(mPBCallback);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	@Override
    	public void onServiceDisconnected(ComponentName name) {
    		// TODO Auto-generated method stub
    		
    	}    	
    }
	
	
	// This class implements the callback function which got called from PlaybackService
	private static final class OdysseyNowPlayingReceiver extends IOdysseyNowPlayingCallback.Stub
	{
		private final WeakReference<OdysseyApplication> mApplication;
		
		public OdysseyNowPlayingReceiver(OdysseyApplication application) {
			mApplication = new WeakReference<OdysseyApplication>(application);
		}
		
		@Override
		public void receiveNewNowPlayingInformation(NowPlayingInformation nowPlaying) throws RemoteException {
			Log.v(TAG,"Received new playing information: " + nowPlaying );
		}
		
	}
	
	public void finalize() {
		// Remove callback object or it will get really nasty for the PlaybackService
		try {
			mPlaybackService.unregisterNowPlayingReceiver(mPBCallback);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Close service connection
		mPBServiceConnection = null;
	}
}
