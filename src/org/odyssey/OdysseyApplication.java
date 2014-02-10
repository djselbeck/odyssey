package org.odyssey;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

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

public class OdysseyApplication extends Application {
    private MusicLibraryHelper mLibraryHelper;
    private IOdysseyPlaybackService mPlaybackService = null;
    private PlaybackServiceConnection mPBServiceConnection = null;
    private IOdysseyNowPlayingCallback mPBCallback = null;
    private boolean mConnectionEstablishing = false;

    private static final String TAG = "OdysseyApplication";

    private NowPlayingInformation mLastNowPlaying = null;

    public OdysseyApplication() {
        mNowPlayingListeners = new ArrayList<OdysseyApplication.NowPlayingListener>();
        // mPBServiceConnection = new PlaybackServiceConnection(this);
        Log.v(TAG, "MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());
    }

    public void setLibraryHelper(MusicLibraryHelper helper) {
        if (helper != null) {
            mLibraryHelper = helper;
        }
    }

    public MusicLibraryHelper getLibraryHelper() {
        return mLibraryHelper;
    }

    private void setPlaybackService(IOdysseyPlaybackService service) {
        if (service != null) {
            mPlaybackService = service;
        }
    }

    public IOdysseyPlaybackService getPlaybackService() {
        Log.v(TAG, "Playback service requested");

        if (mPlaybackService == null && !mConnectionEstablishing) {
            Log.v(TAG, "Reopening service connection");
            mPBServiceConnection = new PlaybackServiceConnection(this);
            mConnectionEstablishing = true;
            // Create initial service connection here
            // create service connection
            Intent serviceStartIntent = new Intent(this, PlaybackService.class);
            // startService(serviceStartIntent);
            bindService(serviceStartIntent, mPBServiceConnection, Context.BIND_AUTO_CREATE);
        }
        return mPlaybackService;
    }

    private class PlaybackServiceConnection implements ServiceConnection {

        private OdysseyApplication mApplication;

        public PlaybackServiceConnection(OdysseyApplication application) {
            mApplication = application;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "Service connection created");
            if (mPlaybackService == null) {
                setPlaybackService(IOdysseyPlaybackService.Stub.asInterface(service));
            }
            // Create callback connection
            // PlaybackService -> OdysseyApplication
            try {
                if (mPBCallback == null) {
                    mPBCallback = new OdysseyNowPlayingReceiver(mApplication);
                }
                mPlaybackService.registerNowPlayingReceiver(mPBCallback);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mConnectionEstablishing = false;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            Log.v(TAG, "Service connection lost");
            Intent serviceStartIntent = new Intent(mApplication, PlaybackService.class);
            // startService(serviceStartIntent);
            bindService(serviceStartIntent, mPBServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    // This class implements the callback function which got called from
    // PlaybackService
    // Interface implementation for IOdysseyNowPlayingCallback.aidl
    private static final class OdysseyNowPlayingReceiver extends IOdysseyNowPlayingCallback.Stub {
        private final WeakReference<OdysseyApplication> mApplication;

        public OdysseyNowPlayingReceiver(OdysseyApplication application) {
            mApplication = new WeakReference<OdysseyApplication>(application);
        }

        @Override
        public void receiveNewNowPlayingInformation(NowPlayingInformation nowPlaying) throws RemoteException {
            Log.v(TAG, "Received new playing information: " + MusicLibraryHelper.getTrackItemFromURL(nowPlaying.getPlayingURL(), mApplication.get().getContentResolver()));
            mApplication.get().notifyNowPlaying(nowPlaying);
        }

    }

    // Clear up connections to service otherwise IT WILL crash
    public void finalize() {
        // Remove callback object or it will get really nasty for the
        // PlaybackService
        try {
            mPlaybackService.unregisterNowPlayingReceiver(mPBCallback);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Close service connection
        mPBServiceConnection = null;
    }

    // Callback functions/attributes for rest of application
    // Used for notifying other parts of the main gui
    // OdysseyApplication --> MainActivity
    private ArrayList<NowPlayingListener> mNowPlayingListeners;

    public synchronized void registerNowPlayingListener(NowPlayingListener listener) {
        Log.v(TAG, "added new nowplayinglistener in mainapplication");
        mNowPlayingListeners.add(listener);
        // Notify about last information
        if (mLastNowPlaying != null) {
            listener.onNewInformation(mLastNowPlaying);
        }
    }

    public synchronized void unregisterNowPlayingListener(NowPlayingListener listener) {
        mNowPlayingListeners.remove(listener);
    }

    // Notifies connected callback listeners, like labels
    public synchronized void notifyNowPlaying(NowPlayingInformation info) {
        mLastNowPlaying = info;
        for (NowPlayingListener listener : mNowPlayingListeners) {
            Log.v(TAG, "Notifying application nowplaying listener");
            listener.onNewInformation(info);
        }
    }

    // Interface specification for NowPlaying listeners
    public interface NowPlayingListener {
        public void onNewInformation(NowPlayingInformation info);
    }
}
