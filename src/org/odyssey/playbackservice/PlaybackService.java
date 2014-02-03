package org.odyssey.playbackservice;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.odyssey.IOdysseyNowPlayingCallback;
import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.NowPlayingInformation;
import org.odyssey.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class PlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {

	public static final String TAG = "OdysseyPlaybackService";
	public static final int NOTIFICATION_ID = 42;

	public static final String ACTION_TESTPLAY = "org.odyssey.testplay";
	public static final String ACTION_PLAY = "org.odyssey.play";
	public static final String ACTION_PAUSE = "org.odyssey.pause";
	public static final String ACTION_NEXT = "org.odyssey.next";
	public static final String ACTION_PREVIOUS = "org.odyssey.previous";
	public static final String ACTION_SEEKTO = "org.odyssey.seekto";
	public static final String ACTION_STOP = "org.odyssey.stop";
	public static final String ACTION_QUIT = "org.odyssey.quit";

	private HandlerThread mHandlerThread;
	private PlaybackServiceHandler mHandler;
	
	private boolean mLostAudioFocus = false;

	// Notification objects
	NotificationManager mNotificationManager;
	NotificationCompat.Builder mNotificationBuilder;
	Notification mNotification;

	// Mediaplayback stuff
	private GaplessPlayer mPlayer;
	private ArrayList<TrackItem> mCurrentList;
	private int mCurrentPlayingIndex;
	private boolean mIsDucked = false;

	// NowPlaying callbacks
	// List holding registered callback clients
	private ArrayList<IOdysseyNowPlayingCallback> mNowPlayingCallbacks;

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "Bind:" + intent.getType());
		return new PlaybackServiceStub(this);
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		Log.v(TAG, "Unbind");
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.v(TAG, "Odyssey PlaybackService onCreate");
		Log.v(TAG, "MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());

		// Start Handlerthread
		mHandlerThread = new HandlerThread("OdysseyHandlerThread", Process.THREAD_PRIORITY_AUDIO);
		mHandlerThread.start();
		mHandler = new PlaybackServiceHandler(mHandlerThread.getLooper(), this);

		// Create MediaPlayer
		mPlayer = new GaplessPlayer(this);
		Log.v(TAG, "Service created");

		// Set listeners
		mPlayer.setOnTrackStartListener(new PlaybackStartListener(this));
		mPlayer.setOnTrackFinishedListener(new PlaybackFinishListener());
		Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

		// Create playlist
		mCurrentList = new ArrayList<TrackItem>();
		mCurrentPlayingIndex = -1;

		// NowPlaying
		mNowPlayingCallbacks = new ArrayList<IOdysseyNowPlayingCallback>();

		mNotificationBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_odys).setContentTitle("Odyssey").setContentText("");
		
		registerReceiver(mNoisyReceiver, new IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "onStartCommand");
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
	}

	// Directly plays uri
	public void playURI(TrackItem track) {
		// Clear playlist, enqueue uri, jumpto 0
		clearPlaylist();
		enqueueTrack(track);
		jumpToIndex(0);
	}

	// Stops all playback
	public void stop() {
		mPlayer.stop();
		mCurrentPlayingIndex = -1;
		// Send empty NowPlaying
		broadcastNowPlaying(new NowPlayingInformation(0, "", -1));
		stopService();
	}

	public void pause() {
		if (mPlayer.isRunning()) {
			mPlayer.pause();
		}
		broadcastNowPlaying(new NowPlayingInformation(0, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));
	}

	public void resume() {
		if (mCurrentPlayingIndex < 0 && mCurrentList.size() > 0) {
			// Songs existing so start playback of playlist begin
			jumpToIndex(0);
		} else if ( mCurrentPlayingIndex < 0 && mCurrentList.size() == 0 ) {
			broadcastNowPlaying(new NowPlayingInformation(0, "", -1 ));
		} else  {
			mPlayer.resume();
			broadcastNowPlaying(new NowPlayingInformation(1, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));
		}
	}

	public void togglePause() {
		// Toggles playback state
		if (mPlayer.isRunning()) {
			pause();
		} else {
			resume();
		}
	}

	/**
	 * Sets nextplayback track to following on in playlist
	 */
	public void setNextTrack() {
		// Needs to set gaplessplayer next object and reorganize playlist
		mPlayer.stop();
		mCurrentPlayingIndex++;

		// Next track is availible
		if (mCurrentPlayingIndex < mCurrentList.size()) {
			// Start playback of new song
			try {
				mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
				// Check if next song is availible (gapless)
				if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
					mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sets nextplayback track to preceding on in playlist
	 */
	public void setPreviousTrack() {
		// Needs to set gaplessplayer next object and reorganize playlist
		mPlayer.stop();
		if (mCurrentPlayingIndex - 1 >= 0)
			mCurrentPlayingIndex--;

		// Next track is availible
		if (mCurrentPlayingIndex < mCurrentList.size()) {
			// Start playback of new song
			try {
				mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
				// Check if next song is availible (gapless)
				if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
					mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private PlaybackServiceHandler getHandler() {
		return mHandler;
	}

	public List<TrackItem> getCurrentList() {
		return mCurrentList;
	}

	public void clearPlaylist() {
		// Stop the playback
		mPlayer.stop();
		// Clear the list and reset index
		mCurrentList.clear();
		mCurrentPlayingIndex = -1;
		broadcastNowPlaying(new NowPlayingInformation(0, "", mCurrentPlayingIndex));

		// TODO notify connected listeners
	}

	public void jumpToIndex(int index) {
		Log.v(TAG, "Playback of index: " + index + " requested");
		Log.v(TAG, "Playlist size: " + mCurrentList.size());
		// Stop playback
		mPlayer.stop();
		// Set currentindex to new song
		if (index < mCurrentList.size()) {
			mCurrentPlayingIndex = index;
			try {
				Log.v(TAG, "Start playback of: " + mCurrentList.get(mCurrentPlayingIndex));
				// Request audio focus before doing anything
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
				if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
					// Abort command
					return;
				}
				mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());

				broadcastNowPlaying(new NowPlayingInformation(1, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));

				// Check if another song follows current one for gapless
				// playback
				if ((mCurrentPlayingIndex + 1) < mCurrentList.size()) {
					Log.v(TAG, "Set next track to: " + mCurrentList.get(mCurrentPlayingIndex + 1));
					mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void enqueueTracks(ArrayList<TrackItem> tracklist) {
		// Check if current song is old last one, if so set next song to MP for
		// gapless playback
		mCurrentList.addAll(tracklist);
	}

	public void enqueueTrack(TrackItem track) {
		// Check if current song is old last one, if so set next song to MP for
		// gapless playback
		mCurrentList.add(track);
	}

	public void dequeueTracks(ArrayList<String> tracklist) {
		for (String track : tracklist) {
			dequeueTrack(track);
		}
	}

	public void dequeueTrack(String track) {
		// Check if track is currently playing, if so stop it
		mCurrentList.remove(track);
	}

	public void dequeueTrack(int index) {
		// Check if track is currently playing, if so stop it
		if (mCurrentPlayingIndex == index) {

		}
		if (index < mCurrentList.size()) {
			mCurrentList.remove(index);
		}
	}

	/**
	 * Stops the gapless mediaplayer and cancels the foreground service. Removes
	 * any ongoing notification.
	 */
	public void stopService() {
		mPlayer.stop();
		stopForeground(true);
		mNotificationBuilder.setOngoing(false);
		mNotificationManager.cancel(NOTIFICATION_ID);
		stopSelf();
	}

	/**
	 * Registers callback interfaces from distant processes which receive the
	 * NowPlayingInformation
	 * 
	 * @param callback
	 */
	public void registerNowPlayingCallback(IOdysseyNowPlayingCallback callback) {
		Log.v(TAG, "Added NowPlaying callback");
		mNowPlayingCallbacks.add(callback);

		// Notify about current status right away
		if (mCurrentList.size() > 0) {
			String playingURL = mCurrentList.get(mCurrentPlayingIndex).getTrackURL();
			int playing = mPlayer.isRunning() ? 1 : 0;
			try {
				callback.receiveNewNowPlayingInformation(new NowPlayingInformation(playing, playingURL, mCurrentPlayingIndex));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Unregister callback interfaces from distant processes
	 * 
	 * @param callback
	 */
	public void unregisterNowPlayingCallback(IOdysseyNowPlayingCallback callback) {
		Log.v(TAG, "Unregistering callback");
		mNowPlayingCallbacks.remove(callback);
	}

	private void broadcastNowPlaying(NowPlayingInformation info) {
		/*
		 * Sends a new NowPlaying object on its way to connected callbacks
		 * PlaybackService --> OdysseyApplication |-> Homescreen-widget
		 */
		for (IOdysseyNowPlayingCallback callback : mNowPlayingCallbacks) {
			Log.v(TAG, "Sending now playing information to receiver");
			try {
				callback.receiveNewNowPlayingInformation(info);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void updateNotification() {
		Intent resultIntent = new Intent(this, MainActivity.class);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(resultIntent);

		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		String url = mCurrentList.get(mCurrentPlayingIndex).getTrackURL();
		TrackItem trackItem = MusicLibraryHelper.getTrackItemFromURL(url, getContentResolver());
		mNotificationBuilder.setContentTitle(trackItem.getTrackTitle());
		mNotificationBuilder.setContentText(trackItem.getTrackArtist());
		mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

		NotificationCompat.BigTextStyle notificationStyle = new NotificationCompat.BigTextStyle();
		notificationStyle.bigText(trackItem.getTrackTitle());
		mNotificationBuilder.setStyle(notificationStyle);
		// mNotificationBuilder.addAction(android.R.drawable.ic_media_pause,
		// "Pause", null);
		mNotificationBuilder.setContentIntent(resultPendingIntent);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// Make notification persistent
		mNotificationBuilder.setOngoing(true);
		mNotification = mNotificationBuilder.build();
		mNotificationManager.notify(NOTIFICATION_ID, mNotification);
		startForeground(NOTIFICATION_ID, mNotification);
	}

	private final static class PlaybackServiceStub extends IOdysseyPlaybackService.Stub {
		// Holds the actuall playback service for handling reasons
		private final WeakReference<PlaybackService> mService;

		public PlaybackServiceStub(PlaybackService service) {
			mService = new WeakReference<PlaybackService>(service);
		}

		/*
		 * Following are methods which call the handler thread (which runs at
		 * audio priority) so that handling of playback is done in a seperate
		 * thread for performance reasons.
		 */
		@Override
		public void play(TrackItem track) throws RemoteException {
			// Create play control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAY, track);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void pause() throws RemoteException {
			// Create pause control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PAUSE);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void stop() throws RemoteException {
			// Create stop control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_STOP);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void setNextTrack(String uri) throws RemoteException {
			// Create nexttrack control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SETNEXTRACK, uri);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void enqueueTracks(List<TrackItem> tracks) throws RemoteException {
			// Create enqueuetracks control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACKS, (ArrayList<TrackItem>) tracks);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void enqueueTrack(TrackItem track) throws RemoteException {
			// Create enqueuetrack control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACK, track);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void dequeueTrack(TrackItem track) throws RemoteException {
			// Create dequeuetrack control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACK, track);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void dequeueTracks(List<TrackItem> tracks) throws RemoteException {
			// Create dequeuetracks control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACKS, (ArrayList<TrackItem>) tracks);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void getCurrentList(List<TrackItem> list) throws RemoteException {
			for (TrackItem trackItem : mService.get().getCurrentList()) {
				Log.v(TAG, "Returning: " + trackItem);
				list.add(trackItem);
			}
		}

		@Override
		public void setRandom(boolean random) throws RemoteException {
			// Create random control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RANDOM, random);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void setRepeat(boolean repeat) throws RemoteException {
			// Create repeat control object
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_REPEAT, repeat);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public String getArtist() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAlbum() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getTrackname() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getTrackNo() throws RemoteException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getBitrate() throws RemoteException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getSamplerate() throws RemoteException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void seekTo(int position) throws RemoteException {
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SEEKTO, position);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void jumpTo(int position) throws RemoteException {
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_JUMPTO, position);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void clearPlaylist() throws RemoteException {
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_CLEARPLAYLIST);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void resume() throws RemoteException {
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RESUME);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void next() throws RemoteException {
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_NEXT);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void previous() throws RemoteException {
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PREVIOUS);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}

		@Override
		public void registerNowPlayingReceiver(IOdysseyNowPlayingCallback receiver) throws RemoteException {
			mService.get().registerNowPlayingCallback(receiver);
		}

		@Override
		public void unregisterNowPlayingReceiver(IOdysseyNowPlayingCallback receiver) throws RemoteException {
			mService.get().unregisterNowPlayingCallback(receiver);
		}

		@Override
		public void togglePause() throws RemoteException {
			ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_TOGGLEPAUSE);
			Message msg = mService.get().getHandler().obtainMessage();
			msg.obj = obj;
			mService.get().getHandler().sendMessage(msg);
		}
	}

	private class PlaybackStartListener implements GaplessPlayer.OnTrackStartedListener {
		private PlaybackService mPlaybackService;

		public PlaybackStartListener(PlaybackService service) {
			mPlaybackService = service;
		}

		@Override
		public void onTrackStarted(String URI) {
			Log.v(TAG, "track started: " + URI);
			broadcastNowPlaying(new NowPlayingInformation(1, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));
			updateNotification();
		}
	}

	private class PlaybackFinishListener implements GaplessPlayer.OnTrackFinishedListener {

		@Override
		public void onTrackFinished() {
			Log.v(TAG, "Playback of index: " + mCurrentPlayingIndex + " finished ");
			// Forward current index and set new song for gapless playback
			// if new song is availible
			mCurrentPlayingIndex++;
			if (mCurrentPlayingIndex < mCurrentList.size()) {
				broadcastNowPlaying(new NowPlayingInformation(1, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));
				updateNotification();
			}
			if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
				try {
					mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		// TODO Auto-generated method stub
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN:
			Log.v(TAG, "Gained audiofocus");
			if (mIsDucked) {
				mPlayer.setVolume(1.0f, 1.0f);
				mIsDucked = false;
			} else if ( mLostAudioFocus) {
				mPlayer.resume();
				mLostAudioFocus = false;
			}
			break;
		case AudioManager.AUDIOFOCUS_LOSS:
			Log.v(TAG, "Lost audiofocus");
			// Stop playback completely and release resources
			stopService();
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			Log.v(TAG, "Lost audiofocus temporarily");
			// Pause audio for the moment of focus loss
			if ( mPlayer.isRunning() ) {
				mPlayer.pause();
				mLostAudioFocus = true;
			}
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			Log.v(TAG, "Lost audiofocus temporarily duckable");
			if (mPlayer.isRunning()) {
				mPlayer.setVolume(0.1f, 0.1f);
				mIsDucked = true;
			}
			break;
		default:
			return;
		}
		


	}
	
	private final BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
	                android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
				Log.v(TAG,"NOISY AUDIO! CANCEL MUSIC");
				pause();
			}
		}
		
	};

}
