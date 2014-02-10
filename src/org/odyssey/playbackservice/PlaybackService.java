package org.odyssey.playbackservice;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

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
    public static final String ACTION_TOGGLEPAUSE = "org.odyssey.togglepause";
    public static final String MESSAGE_NEWTRACKINFORMATION = "org.odyssey.newtrackinfo";

    public static final String INTENT_TRACKITEMNAME = "OdysseyTrackItem";
    public static final String INTENT_NOWPLAYINGNAME = "OdysseyNowPlaying";

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
    private int mNextPlayingIndex;
    private int mLastPlayingIndex;
    private boolean mIsDucked = false;
    private boolean mIsPaused = false;
    private int mLastPosition = 0;

    private boolean mRandom = false;
    private boolean mRepeat = false;

    // Remote control
    private RemoteController mRemoteControlClient = null;
    private String mLastCoverURL = "";

    // Timer for service stop after certain amount of time
    private Timer mServiceCancelTimer = null;
    private WakeLock mTempWakelock = null;

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
        super.onUnbind(intent);
        Log.v(TAG, "Unbind");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Odyssey PlaybackService onCreate");
        Log.v(TAG, "MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());

        // Start Handlerthread
        mHandlerThread = new HandlerThread("OdysseyHandlerThread", Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new PlaybackServiceHandler(mHandlerThread.getLooper(), this);

        // Create MediaPlayer
        mPlayer = new GaplessPlayer(this);
        Log.v(TAG, "Service created");

        // Set listeners
        mPlayer.setOnTrackStartListener(new PlaybackStartListener(this));
        mPlayer.setOnTrackFinishedListener(new PlaybackFinishListener());
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // Create playlist
        mCurrentList = new ArrayList<TrackItem>();
        mCurrentPlayingIndex = -1;
        mLastPlayingIndex = -1;
        mNextPlayingIndex = -1;

        // NowPlaying
        mNowPlayingCallbacks = new ArrayList<IOdysseyNowPlayingCallback>();

        mNotificationBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_odys).setContentTitle("Odyssey").setContentText("");
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.addAction(ACTION_PREVIOUS);
        intentFilter.addAction(ACTION_PAUSE);
        intentFilter.addAction(ACTION_PLAY);
        intentFilter.addAction(ACTION_TOGGLEPAUSE);
        intentFilter.addAction(ACTION_NEXT);
        intentFilter.addAction(ACTION_STOP);

        registerReceiver(mNoisyReceiver, intentFilter);

        // Remote control client
        ComponentName remoteReceiver = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.registerMediaButtonEventReceiver(remoteReceiver);

        Intent buttonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        buttonIntent.setComponent(remoteReceiver);
        PendingIntent buttonPendingIntent = PendingIntent.getBroadcast(this, 0, buttonIntent, 0);

        // Create remotecontrol instance
        mRemoteControlClient = new RemoteController(buttonPendingIntent);
        audioManager.registerRemoteControlClient(mRemoteControlClient);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mTempWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Service destroyed");
        stopSelf();

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
        mLastCoverURL = "";

        // Send empty NowPlaying
        broadcastNowPlaying(new NowPlayingInformation(0, "", -1));
        stopService();
    }

    public void pause() {
        if (mPlayer.isRunning()) {
            mLastPosition = mPlayer.getPosition();
            mPlayer.pause();
            mIsPaused = true;
        }
        broadcastNowPlaying(new NowPlayingInformation(0, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));
        updateNotification();
        mServiceCancelTimer = new Timer();
        // Set timeout to 10 minutes for now
        mServiceCancelTimer.schedule(new ServiceCancelTask(), (long) ((60 * 1000) * 5));
    }

    public void resume() {
        if (mCurrentPlayingIndex < 0 && mCurrentList.size() > 0) {
            // Songs existing so start playback of playlist begin
            jumpToIndex(0);
        } else if (mCurrentPlayingIndex < 0 && mCurrentList.size() == 0) {
            broadcastNowPlaying(new NowPlayingInformation(0, "", -1));
        } else {
            /*
             * Make sure service is "started" so android doesn't handle it as a
             * "bound service"
             */
            Intent serviceStartIntent = new Intent(this, PlaybackService.class);
            serviceStartIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            startService(serviceStartIntent);
            if (mServiceCancelTimer != null) {
                mServiceCancelTimer.cancel();
                mServiceCancelTimer = null;
            }
            mPlayer.resume();
            mIsPaused = false;
            mLastPosition = 0;
            broadcastNowPlaying(new NowPlayingInformation(1, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));
            updateNotification();
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
        // Keep device at least for 5 seconds turned on
        mTempWakelock.acquire(5000);
        mPlayer.stop();
        if (mRandom) {

            // save lastindex for previous
            mLastPlayingIndex = mCurrentPlayingIndex;

            // set currentindex to nextindex if exists
            Random rand = new Random();
            if (mNextPlayingIndex == -1) {
                mCurrentPlayingIndex = rand.nextInt(mCurrentList.size());
            } else {
                mCurrentPlayingIndex = mNextPlayingIndex;
            }

            // set new random nextindex
            mNextPlayingIndex = rand.nextInt(mCurrentList.size());

            // Next track is availible
            if (mCurrentPlayingIndex < mCurrentList.size() && (mCurrentPlayingIndex >= 0)) {
                try {
                    mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
                    // Check if next song is availible (gapless)
                    if (mNextPlayingIndex < mCurrentList.size() && (mNextPlayingIndex >= 0)) {
                        mPlayer.setNextTrack(mCurrentList.get(mNextPlayingIndex).getTrackURL());
                    }

                } catch (IllegalArgumentException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IllegalArgument for playback");
                    Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
                } catch (SecurityException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "SecurityException for playback");
                    Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
                } catch (IllegalStateException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IllegalState for playback");
                    Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IOException for playback");
                    Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
                }
            }

        } else {

            // save lastindex for previous in random mode
            mLastPlayingIndex = mCurrentPlayingIndex;

            if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                mCurrentPlayingIndex++;
            } else if ((mCurrentPlayingIndex + 1) == mCurrentList.size()) {
                if (mRepeat) {
                    // Last track so set index = 0 and repeat playlist
                    mCurrentPlayingIndex = 0;
                } else {
                    // Last track just leave here
                    stop();
                    return;
                }
            }

            // Next track is availible
            if (mCurrentPlayingIndex < mCurrentList.size() && (mCurrentPlayingIndex >= 0)) {
                // Start playback of new song
                try {
                    mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
                    // Check if next song is availible (gapless)
                    if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                        mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
                    }
                } catch (IllegalArgumentException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IllegalArgument for playback");
                    Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
                } catch (SecurityException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "SecurityException for playback");
                    Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
                } catch (IllegalStateException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IllegalState for playback");
                    Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IOException for playback");
                    Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void enqueueAsNextTrack(TrackItem track) {
        // Check if currently playing, than enqueue after current song
        if (mCurrentPlayingIndex >= 0) {
            // Enqueue in list structure
            mCurrentList.add(mCurrentPlayingIndex + 1, track);
            // Set next track to new one
            try {
                mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
            } catch (IllegalArgumentException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IllegalArgument for playback");
                Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "SecurityException for playback");
                Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
            } catch (IllegalStateException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IllegalState for playback");
                Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IOException for playback");
                Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
            }
        } else {
            // If not playing just add it to the beginning of the playlist
            mCurrentList.add(0, track);
            // Start playback which is probably intended
            jumpToIndex(0);
        }
    }

    /**
     * Sets nextplayback track to preceding on in playlist
     */
    public void setPreviousTrack() {
        // Needs to set gaplessplayer next object and reorganize playlist
        // Get wakelock otherwise device could go to deepsleep until new song
        // starts playing

        // Keep device at least for 5 seconds turned on
        mTempWakelock.acquire(5000);

        mPlayer.stop();
        if (mRandom) {

            if (mLastPlayingIndex == -1) {
                // if no lastindex reuse currentindex
                mLastPlayingIndex = mCurrentPlayingIndex;

            }

            // use lastindex for currentindex
            mCurrentPlayingIndex = mLastPlayingIndex;

            // create new random nextindex
            Random rand = new Random();
            mNextPlayingIndex = rand.nextInt(mCurrentList.size());

            // Next track is availible
            if (mCurrentPlayingIndex < mCurrentList.size() && mCurrentPlayingIndex >= 0) {
                try {
                    mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
                    // Check if next song is availible (gapless)
                    if (mNextPlayingIndex < mCurrentList.size() && (mNextPlayingIndex >= 0)) {
                        mPlayer.setNextTrack(mCurrentList.get(mNextPlayingIndex).getTrackURL());
                    }

                } catch (IllegalArgumentException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IllegalArgument for playback");
                    Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
                } catch (SecurityException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "SecurityException for playback");
                    Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
                } catch (IllegalStateException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IllegalState for playback");
                    Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IOException for playback");
                    Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
                }
            }

        } else {

            if (mCurrentPlayingIndex - 1 >= 0) {
                mCurrentPlayingIndex--;
            } else if (mRepeat) {
                // In repeat mode next track is last track of playlist
                mCurrentPlayingIndex = mCurrentList.size() - 1;
            }

            // Next track is availible
            if (mCurrentPlayingIndex < mCurrentList.size() && mCurrentPlayingIndex >= 0) {
                // Start playback of new song
                try {
                    mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
                    // Check if next song is availible (gapless)
                    if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                        mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
                    }
                } catch (IllegalArgumentException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IllegalArgument for playback");
                    Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
                } catch (SecurityException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "SecurityException for playback");
                    Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
                } catch (IllegalStateException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IllegalState for playback");
                    Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    // In case of error stop playback and log error
                    mPlayer.stop();
                    Log.e(TAG, "IOException for playback");
                    Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private PlaybackServiceHandler getHandler() {
        return mHandler;
    }

    public List<TrackItem> getCurrentList() {
        return mCurrentList;
    }

    public int getPlaylistSize() {
        return mCurrentList.size();
    }

    public TrackItem getPlaylistTrack(int index) {
        if ((index >= 0) && (index < mCurrentList.size())) {
            return mCurrentList.get(index);
        }
        return new TrackItem();
    }

    public void clearPlaylist() {
        // Stop the playback
        stop();
        // Clear the list and reset index
        mCurrentList.clear();

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
                /*
                 * Make sure service is "started" so android doesn't handle it
                 * as a "bound service"
                 */
                Intent serviceStartIntent = new Intent(this, PlaybackService.class);
                serviceStartIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                startService(serviceStartIntent);
                if (mServiceCancelTimer != null) {
                    mServiceCancelTimer.cancel();
                    mServiceCancelTimer = null;
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
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IllegalArgument for playback");
                Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "SecurityException for playback");
                Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
            } catch (IllegalStateException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IllegalState for playback");
                Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IOException for playback");
                Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
            }
        }

    }

    public void seekTo(int position) {
        if (mPlayer.isRunning()) {
            mPlayer.seekTo(position);
        }
    }

    public int getTrackPosition() {
        if (!mIsPaused) {
            return mPlayer.getPosition();
        } else {
            return mLastPosition;
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
        int oldSize = mCurrentList.size();
        mCurrentList.add(track);
        /*
         * If currently playing and playing is the last one in old playlist set
         * enqueued one to next one for gapless mediaplayback
         */
        if (mCurrentPlayingIndex == (oldSize - 1) && (mCurrentPlayingIndex >= 0)) {
            // Next song for MP has to be set for gapless mediaplayback
            try {
                mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
            } catch (IllegalArgumentException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IllegalArgument for playback");
                Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "SecurityException for playback");
                Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
            } catch (IllegalStateException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IllegalState for playback");
                Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IOException for playback");
                Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
            }
        }
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
            // Stop playback of currentsong
            stop();
            // Delete song at index
            mCurrentList.remove(index);
            // Jump to next song which should be at index now
            // Jump is safe about playlist length so no need for extra safety
            jumpToIndex(index);
        } else if ((mCurrentPlayingIndex + 1) == index) {
            // Deletion of next song which requires extra handling
            // because of gapless playback, set next song to next on
            mCurrentList.remove(index);
            try {
                mPlayer.setNextTrack(mCurrentList.get(index).getTrackURL());
            } catch (IllegalArgumentException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IllegalArgument for playback");
                Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "SecurityException for playback");
                Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
            } catch (IllegalStateException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IllegalState for playback");
                Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                // In case of error stop playback and log error
                mPlayer.stop();
                Log.e(TAG, "IOException for playback");
                Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
            }
        } else if (index >= 0 && index < mCurrentList.size()) {
            mCurrentList.remove(index);
            // mCurrentIndex is now moved one position up so set variable
            if (index < mCurrentPlayingIndex) {
                mCurrentPlayingIndex--;
            }
        }
        // Send new NowPlaying because playlist changed
        sendUpdateBroadcast();
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

    public boolean getRandom() {
        return mRandom;
    }

    public boolean getRepeat() {
        return mRepeat;
    }

    public void setRepeat(boolean repeat) {
        // TODO SET LOOPING FOR MP
        mRepeat = repeat;
    }

    public void setRandom(boolean random) {
        // TODO set next mp to random one,too
        mRandom = random;
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

        if (mCurrentPlayingIndex >= 0) {
            Intent broadcastIntent = new Intent(MESSAGE_NEWTRACKINFORMATION);
            ArrayList<Parcelable> extraTrackItemList = new ArrayList<Parcelable>();
            extraTrackItemList.add(mCurrentList.get(mCurrentPlayingIndex));
            ArrayList<Parcelable> extraNPList = new ArrayList<Parcelable>();
            extraNPList.add(info);
            broadcastIntent.putParcelableArrayListExtra(INTENT_TRACKITEMNAME, extraTrackItemList);
            broadcastIntent.putParcelableArrayListExtra(INTENT_NOWPLAYINGNAME, extraNPList);
            sendBroadcast(broadcastIntent);

            String where = android.provider.MediaStore.Audio.Albums.ALBUM + "=?";

            String whereVal[] = { mCurrentList.get(mCurrentPlayingIndex).getTrackAlbum() };

            Cursor cursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART }, where, whereVal, "");

            String coverPath = null;
            if (cursor.moveToFirst()) {
                coverPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            }

            BitmapDrawable cover;

            RemoteControlClient.MetadataEditor editor = mRemoteControlClient.editMetadata(false);

            if (coverPath != null) {
                if (coverPath != mLastCoverURL) {
                    mLastCoverURL = coverPath;
                    cover = (BitmapDrawable) BitmapDrawable.createFromPath(mLastCoverURL);
                    editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, cover.getBitmap());
                }

            } else {
                editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, null);
            }

            // Set remote control
            TrackItem item = mCurrentList.get(mCurrentPlayingIndex);

            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, item.getTrackAlbum());
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, item.getTrackArtist());
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, item.getTrackArtist());
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, item.getTrackTitle());
            editor.apply();
            if (mPlayer.isRunning()) {
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            } else {
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
            }
            mRemoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_NEXT | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS);
        } else {
            Intent broadcastIntent = new Intent(MESSAGE_NEWTRACKINFORMATION);
            ArrayList<Parcelable> extraTrackItemList = new ArrayList<Parcelable>();
            extraTrackItemList.add(new TrackItem());
            ArrayList<Parcelable> extraNPList = new ArrayList<Parcelable>();
            extraNPList.add(info);
            broadcastIntent.putParcelableArrayListExtra(INTENT_TRACKITEMNAME, extraTrackItemList);
            broadcastIntent.putParcelableArrayListExtra(INTENT_NOWPLAYINGNAME, extraNPList);
            sendBroadcast(broadcastIntent);
            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);

        }
    }

    public TrackItem getCurrentTrack() {
        if (mCurrentPlayingIndex >= 0 && mCurrentList.size() > mCurrentPlayingIndex) {
            return mCurrentList.get(mCurrentPlayingIndex);
        }
        return null;
    }

    private void sendUpdateBroadcast() {
        if (mPlayer.isRunning()) {
            broadcastNowPlaying(new NowPlayingInformation(1, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));

        } else {
            broadcastNowPlaying(new NowPlayingInformation(0, "", -1));

        }
    }

    private void updateNotification() {
        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_odys).setContentTitle("Odyssey").setContentText("");

        // TODO secure
        String url = mCurrentList.get(mCurrentPlayingIndex).getTrackURL();
        TrackItem trackItem = MusicLibraryHelper.getTrackItemFromURL(url, getContentResolver());
        mNotificationBuilder.setContentTitle(trackItem.getTrackTitle());
        mNotificationBuilder.setContentText(trackItem.getTrackArtist());
        mNotificationBuilder.setSubText(trackItem.getTrackAlbum());

        // Set Notification image to cover if existing
        // if ((mCurrentPlayingIndex >= 0) && (mCurrentPlayingIndex <
        // mCurrentList.size())) {
        // String where = android.provider.MediaStore.Audio.Albums.ALBUM + "=?";
        //
        // String whereVal[] = {
        // mCurrentList.get(mCurrentPlayingIndex).getTrackAlbum() };
        //
        // Cursor cursor =
        // getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
        // new String[] { MediaStore.Audio.Albums.ALBUM_ART }, where, whereVal,
        // "");
        //
        // String coverPath = null;
        // if (cursor.moveToFirst()) {
        // coverPath =
        // cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        // }
        //
        // if (coverPath != null) {
        // BitmapDrawable cover = (BitmapDrawable)
        // BitmapDrawable.createFromPath(coverPath);
        // Bitmap coverBitmap =
        // Bitmap.createBitmap(cover.getBitmap(),0,0,120,120);
        //
        // mNotificationBuilder.setLargeIcon(coverBitmap);
        // } else {
        // mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(),
        // R.drawable.ic_launcher));
        // }
        // } else {
        // mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(),
        // R.drawable.ic_launcher));
        // }
        mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        // NotificationCompat.BigTextStyle notificationStyle = new
        // NotificationCompat.BigTextStyle();
        // notificationStyle.setBigContentTitle(trackItem.getTrackTitle());
        // notificationStyle.bigText(trackItem.getTrackAlbum() + "\n" +
        // trackItem.getTrackArtist());
        // mNotificationBuilder.setStyle(notificationStyle);

        // Previous song action
        Intent prevIntent = new Intent(ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 42, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.addAction(android.R.drawable.ic_media_previous, "", prevPendingIntent);

        // Pause/Play action
        if (mPlayer.isRunning()) {
            Intent pauseIntent = new Intent(ACTION_PAUSE);
            PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 42, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.addAction(android.R.drawable.ic_media_pause, "", pausePendingIntent);
        } else {
            Intent playIntent = new Intent(ACTION_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 42, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.addAction(android.R.drawable.ic_media_play, "", playPendingIntent);
        }

        // Previous song action
        Intent nextIntent = new Intent(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 42, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.addAction(android.R.drawable.ic_media_next, "", nextPendingIntent);

        mNotificationBuilder.setContentIntent(resultPendingIntent);

        // Quit action
        Intent stopIntent = new Intent(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 42, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationBuilder.setDeleteIntent(stopPendingIntent);
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

        @Override
        public int getTrackPosition() throws RemoteException {
            return mService.get().getTrackPosition();
        }

        @Override
        public int getTrackDuration() throws RemoteException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean getRandom() throws RemoteException {
            return mService.get().getRandom();
        }

        @Override
        public boolean getRepeat() throws RemoteException {
            return mService.get().getRepeat();
        }

        @Override
        public TrackItem getCurrentSong() throws RemoteException {
            return mService.get().getCurrentTrack();
        }

        @Override
        public void dequeueTrackIndex(int index) throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUEINDEX, index);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public TrackItem getPlaylistSong(int index) throws RemoteException {
            return mService.get().getPlaylistTrack(index);
        }

        @Override
        public int getPlaylistSize() throws RemoteException {
            return mService.get().getPlaylistSize();
        }

        @Override
        public void enqueueTrackAsNext(TrackItem track) throws RemoteException {
            // Create nexttrack control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYNEXT, track);
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
            Log.v(TAG, "track started: " + URI + " PL index: " + mCurrentPlayingIndex);
            broadcastNowPlaying(new NowPlayingInformation(1, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));
            updateNotification();
            if (mTempWakelock.isHeld()) {
                // we could release wakelock here already
                mTempWakelock.release();
            }
        }
    }

    private class PlaybackFinishListener implements GaplessPlayer.OnTrackFinishedListener {

        @Override
        public void onTrackFinished() {
            Log.v(TAG, "Playback of index: " + mCurrentPlayingIndex + " finished ");
            // Check if random is active
            if (mRandom) {
                Random rand = new Random();

                // save lastindex for previous
                mLastPlayingIndex = mCurrentPlayingIndex;

                // set currentindex to nextindex if exists
                if (mNextPlayingIndex == -1) {
                    // create new random index
                    mCurrentPlayingIndex = rand.nextInt(mCurrentList.size());
                } else {
                    mCurrentPlayingIndex = mNextPlayingIndex;
                }
                broadcastNowPlaying(new NowPlayingInformation(1, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));
                updateNotification();

                // create new random nextindex
                mNextPlayingIndex = rand.nextInt(mCurrentList.size());

                // set next track for gapless playback
                if (mNextPlayingIndex < mCurrentList.size() && (mNextPlayingIndex >= 0)) {
                    try {
                        mPlayer.setNextTrack(mCurrentList.get(mNextPlayingIndex).getTrackURL());
                    } catch (IllegalArgumentException e) {
                        // In case of error stop playback and log error
                        mPlayer.stop();
                        Log.e(TAG, "IllegalArgument for playback");
                        Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
                    } catch (SecurityException e) {
                        // In case of error stop playback and log error
                        mPlayer.stop();
                        Log.e(TAG, "SecurityException for playback");
                        Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
                    } catch (IllegalStateException e) {
                        // In case of error stop playback and log error
                        mPlayer.stop();
                        Log.e(TAG, "IllegalState for playback");
                        Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        // In case of error stop playback and log error
                        mPlayer.stop();
                        Log.e(TAG, "IOException for playback");
                        Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
                    }
                }
            } else {

                // save lastindex for previous in random mode
                mLastPlayingIndex = mCurrentPlayingIndex;

                // Check if this song was the last one in the playlist
                if ((mCurrentPlayingIndex + 1) == mCurrentList.size()) {
                    if (mRepeat) {
                        // Was last song in list so repeat playlist
                        jumpToIndex(0);
                    } else {
                        // Was last song in list stop everything
                        Log.v(TAG, "Last song played");
                        stop();
                    }
                } else {
                    // At least one song to go
                    mCurrentPlayingIndex++;
                    broadcastNowPlaying(new NowPlayingInformation(1, mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), mCurrentPlayingIndex));
                    updateNotification();

                    /*
                     * Check if we even have one more song to play if it is the
                     * case, schedule it for next playback (gapless playback)
                     */
                    if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                        try {
                            mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
                        } catch (IllegalArgumentException e) {
                            // In case of error stop playback and log error
                            mPlayer.stop();
                            Log.e(TAG, "IllegalArgument for playback");
                            Toast.makeText(getBaseContext(), "Playback illegal argument  error", Toast.LENGTH_LONG).show();
                        } catch (SecurityException e) {
                            // In case of error stop playback and log error
                            mPlayer.stop();
                            Log.e(TAG, "SecurityException for playback");
                            Toast.makeText(getBaseContext(), "Playback security error", Toast.LENGTH_LONG).show();
                        } catch (IllegalStateException e) {
                            // In case of error stop playback and log error
                            mPlayer.stop();
                            Log.e(TAG, "IllegalState for playback");
                            Toast.makeText(getBaseContext(), "Playback state error", Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            // In case of error stop playback and log error
                            mPlayer.stop();
                            Log.e(TAG, "IOException for playback");
                            Toast.makeText(getBaseContext(), "Playback IO error", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

        }

    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
        case AudioManager.AUDIOFOCUS_GAIN:
            Log.v(TAG, "Gained audiofocus");
            if (mIsDucked) {
                mPlayer.setVolume(1.0f, 1.0f);
                mIsDucked = false;
            } else if (mLostAudioFocus) {
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
            if (mPlayer.isRunning()) {
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
            if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                Log.v(TAG, "NOISY AUDIO! CANCEL MUSIC");
                pause();
            } else if (intent.getAction().equals(ACTION_PLAY)) {
                resume();
            } else if (intent.getAction().equals(ACTION_PAUSE)) {
                pause();
            } else if (intent.getAction().equals(ACTION_NEXT)) {
                setNextTrack();
            } else if (intent.getAction().equals(ACTION_PREVIOUS)) {
                setPreviousTrack();
            } else if (intent.getAction().equals(ACTION_STOP)) {
                stop();
            } else if (intent.getAction().equals(ACTION_TOGGLEPAUSE)) {
                togglePause();
            }
        }

    };

    private class ServiceCancelTask extends TimerTask {

        @Override
        public void run() {
            stop();
        }

    }

    private class RemoteController extends RemoteControlClient {

        public RemoteController(PendingIntent mediaButtonIntent) {
            super(mediaButtonIntent);
            // TODO Auto-generated constructor stub
        }

    }

}
