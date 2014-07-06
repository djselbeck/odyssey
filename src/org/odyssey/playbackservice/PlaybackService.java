package org.odyssey.playbackservice;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.odyssey.IOdysseyNowPlayingCallback;
import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.NowPlayingInformation;
import org.odyssey.R;
import org.odyssey.manager.DatabaseManager;
import org.odyssey.playbackservice.GaplessPlayer.PlaybackException;

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
import android.util.Log;
import android.widget.Toast;

public class PlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {

    // enums for random, repeat state
    public static enum RANDOMSTATE {
        RANDOM_OFF, RANDOM_ON;
    }

    public static enum REPEATSTATE {
        REPEAT_OFF, REPEAT_ALL, REPEAT_TRACK;
    }

    public static enum PLAYSTATE {
        PLAYING, PAUSE, STOPPED;
    }

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
    private Random mRandomGenerator;

    private int mRandom = 0;
    private int mRepeat = 0;

    // Remote control
    private RemoteController mRemoteControlClient = null;
    private String mLastCoverURL = "";

    // Timer for service stop after certain amount of time
    private Timer mServiceCancelTimer = null;
    private WakeLock mTempWakelock = null;

    // NowPlaying callbacks
    // List holding registered callback clients
    private ArrayList<IOdysseyNowPlayingCallback> mNowPlayingCallbacks;

    // Playlistmanager for saving and reading playlist
    private DatabaseManager mPlaylistManager = null;

    // Mutex for NowPlaying callbacks
    private final Semaphore mCallbackMutex = new Semaphore(1, true);

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

        // set up playlistmanager
        mPlaylistManager = new DatabaseManager(getApplicationContext());

        // read playlist from database
        mCurrentList = mPlaylistManager.readPlaylist();

        // mCurrentList = new ArrayList<TrackItem>();
        mCurrentPlayingIndex = (int) mPlaylistManager.getLastTrackNumber();

        if (mCurrentPlayingIndex < 0 || mCurrentPlayingIndex > mCurrentList.size()) {
            mCurrentPlayingIndex = -1;
        }

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

        // set up random generator
        mRandomGenerator = new Random();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(TAG, "onStartCommand");
        return START_NOT_STICKY;
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
        jumpToIndex(0, true);
    }

    // Stops all playback
    public void stop() {
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
            // Broadcast simple.last.fm.scrobble broadcast
            TrackItem item = mCurrentList.get(mCurrentPlayingIndex);
            Log.v(TAG, "Send to SLS: " + item);
            Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
            bCast.putExtra("state", 3);
            bCast.putExtra("app-name", "Odyssey");
            bCast.putExtra("app-package", "org.odyssey");
            bCast.putExtra("artist", item.getTrackArtist());
            bCast.putExtra("album", item.getTrackAlbum());
            bCast.putExtra("track", item.getTrackTitle());
            bCast.putExtra("duration", item.getTrackDuration() / 1000);
            sendBroadcast(bCast);
        }

        mPlayer.stop();
        mCurrentPlayingIndex = -1;
        mLastCoverURL = "";

        mNextPlayingIndex = -1;
        mLastPlayingIndex = -1;

        updateStatus();

        stopService();
    }

    public void pause() {
        if (mPlayer.isRunning()) {
            mLastPosition = mPlayer.getPosition();
            mPlayer.pause();
            // Save position in settings table
            mPlaylistManager.saveCurrentPlayState(mLastPosition, mCurrentPlayingIndex);

            // Broadcast simple.last.fm.scrobble broadcast
            if (mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
                TrackItem item = mCurrentList.get(mCurrentPlayingIndex);
                Log.v(TAG, "Send to SLS: " + item);
                Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                bCast.putExtra("state", 2);
                bCast.putExtra("app-name", "Odyssey");
                bCast.putExtra("app-package", "org.odyssey");
                bCast.putExtra("artist", item.getTrackArtist());
                bCast.putExtra("album", item.getTrackAlbum());
                bCast.putExtra("track", item.getTrackTitle());
                bCast.putExtra("duration", item.getTrackDuration() / 1000);
                sendBroadcast(bCast);
            }

            mIsPaused = true;
        }

        updateStatus();

        mServiceCancelTimer = new Timer();
        // Set timeout to 10 minutes for now
        mServiceCancelTimer.schedule(new ServiceCancelTask(), (long) ((60 * 1000) * 10));
    }

    public void resume() {
        // Check if mediaplayer needs preparing
        long lastPosition = mPlaylistManager.getLastTrackPosition();
        if (!mPlayer.isPrepared() && (lastPosition != 0) && (mCurrentPlayingIndex != -1) && (mCurrentPlayingIndex < mCurrentList.size())) {
            jumpToIndex(mCurrentPlayingIndex, false, (int) lastPosition);
            Log.v(TAG, "Resuming position before playback to: " + lastPosition);
            return;
        }

        if (mCurrentPlayingIndex < 0 && mCurrentList.size() > 0) {
            // Songs existing so start playback of playlist begin
            jumpToIndex(0, true);
        } else if (mCurrentPlayingIndex < 0 && mCurrentList.size() == 0) {
            updateStatus();
        } else if (mCurrentPlayingIndex < mCurrentList.size()) {
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

            // Broadcast simple.last.fm.scrobble broadcast
            TrackItem item = mCurrentList.get(mCurrentPlayingIndex);
            Log.v(TAG, "Send to SLS: " + item);
            Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
            bCast.putExtra("state", 1);
            bCast.putExtra("app-name", "Odyssey");
            bCast.putExtra("app-package", "org.odyssey");
            bCast.putExtra("artist", item.getTrackArtist());
            bCast.putExtra("album", item.getTrackAlbum());
            bCast.putExtra("track", item.getTrackTitle());
            bCast.putExtra("duration", item.getTrackDuration() / 1000);
            sendBroadcast(bCast);

            mIsPaused = false;
            mLastPosition = 0;

            updateStatus();
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

    // add all tracks to playlist, shuffle and play
    public void playAllTracks() {

        // clear playlist
        clearPlaylist();

        // stop service
        stop();

        // get all tracks
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, "", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");

        // add all tracks to playlist
        if (cursor.moveToFirst()) {

            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));

            TrackItem item = new TrackItem(title, artist, album, url, no, duration, albumKey);

            mCurrentList.add(item);

            // start playing
            jumpToIndex(0, true);

            while (cursor.moveToNext()) {

                title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));

                item = new TrackItem(title, artist, album, url, no, duration, albumKey);

                mCurrentList.add(item);

            }
        }

        cursor.close();

        // shuffle playlist
        shufflePlaylist();
    }

    // shuffle the current playlist
    public void shufflePlaylist() {

        // save currentindex
        int index = mCurrentPlayingIndex;

        if (mCurrentList.size() > 0 && index >= 0 && (index < mCurrentList.size())) {
            // get the current trackitem and remove it from playlist
            TrackItem currentItem = mCurrentList.get(index);
            mCurrentList.remove(index);

            // shuffle playlist and set currentitem as first element
            Collections.shuffle(mCurrentList);
            mCurrentList.add(0, currentItem);

            // reset index
            mCurrentPlayingIndex = 0;

            updateStatus();

            // set next track for gapless

            try {
                mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
            } catch (PlaybackException e) {
                handlePlaybackException(e);
            }
        } else if (mCurrentList.size() > 0 && index < 0) {
            // service stopped just shuffle playlist
            Collections.shuffle(mCurrentList);

            // sent broadcast
            // sendUpdateBroadcast();
            updateStatus();
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
        if (mRandom == RANDOMSTATE.RANDOM_ON.ordinal()) {

            // save lastindex for previous
            mLastPlayingIndex = mCurrentPlayingIndex;

            // set currentindex to nextindex if exists

            if (mNextPlayingIndex == -1) {
                mCurrentPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());

                // if current index dont change create a new random index
                // but just trying 20 times
                int counter = 0;
                while (mLastPlayingIndex == mCurrentPlayingIndex && counter > 20) {
                    mCurrentPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
                    counter++;
                }
            } else {
                mCurrentPlayingIndex = mNextPlayingIndex;
            }

            // set new random nextindex
            mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());

            // if next index equal to current index create a new random index
            // but just trying 20 times
            int counter = 0;
            while (mNextPlayingIndex == mCurrentPlayingIndex && counter > 20) {
                mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
                counter++;
            }

            // Next track is availible
            if (mCurrentPlayingIndex < mCurrentList.size() && (mCurrentPlayingIndex >= 0)) {
                try {
                    mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
                } catch (PlaybackException e) {
                    handlePlaybackException(e);
                }

                // Broadcast simple.last.fm.scrobble broadcast
                TrackItem item = mCurrentList.get(mCurrentPlayingIndex);
                Log.v(TAG, "Send to SLS: " + item);
                Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                bCast.putExtra("state", 0);
                bCast.putExtra("app-name", "Odyssey");
                bCast.putExtra("app-package", "org.odyssey");
                bCast.putExtra("artist", item.getTrackArtist());
                bCast.putExtra("album", item.getTrackAlbum());
                bCast.putExtra("track", item.getTrackTitle());
                bCast.putExtra("duration", item.getTrackDuration() / 1000);
                sendBroadcast(bCast);

                // Check if next song is availible (gapless)
                if (mNextPlayingIndex < mCurrentList.size() && (mNextPlayingIndex >= 0)) {
                    try {
                        mPlayer.setNextTrack(mCurrentList.get(mNextPlayingIndex).getTrackURL());
                    } catch (PlaybackException e) {
                        handlePlaybackException(e);
                    }
                }
            }

        } else {

            // save lastindex for previous in random mode
            mLastPlayingIndex = mCurrentPlayingIndex;

            if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                mCurrentPlayingIndex++;
            } else if ((mCurrentPlayingIndex + 1) == mCurrentList.size()) {
                if (mRepeat == REPEATSTATE.REPEAT_ALL.ordinal()) {
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
                } catch (PlaybackException e) {
                    handlePlaybackException(e);
                }

                // Broadcast simple.last.fm.scrobble broadcast
                TrackItem item = mCurrentList.get(mCurrentPlayingIndex);
                Log.v(TAG, "Send to SLS: " + item);
                Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                bCast.putExtra("state", 0);
                bCast.putExtra("app-name", "Odyssey");
                bCast.putExtra("app-package", "org.odyssey");
                bCast.putExtra("artist", item.getTrackArtist());
                bCast.putExtra("album", item.getTrackAlbum());
                bCast.putExtra("track", item.getTrackTitle());
                bCast.putExtra("duration", item.getTrackDuration() / 1000);
                sendBroadcast(bCast);

                // Check if next song is availible (gapless)
                if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                    try {
                        mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
                    } catch (PlaybackException e) {
                        handlePlaybackException(e);
                    }
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
            } catch (PlaybackException e) {
                handlePlaybackException(e);
            }
        } else {
            // If not playing just add it to the beginning of the playlist
            mCurrentList.add(0, track);
            // Start playback which is probably intended
            jumpToIndex(0, true);
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
        if (mRandom == RANDOMSTATE.RANDOM_ON.ordinal()) {

            if (mLastPlayingIndex == -1) {
                // if no lastindex reuse currentindex
                mLastPlayingIndex = mCurrentPlayingIndex;

            }

            // use lastindex for currentindex
            mCurrentPlayingIndex = mLastPlayingIndex;

            // create new random nextindex
            mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());

            // if next index equal to current index create a new random index
            // but just trying 20 times
            int counter = 0;
            while (mNextPlayingIndex == mCurrentPlayingIndex && counter > 20) {
                mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
                counter++;
            }

            // Next track is availible
            if (mCurrentPlayingIndex < mCurrentList.size() && mCurrentPlayingIndex >= 0) {
                try {
                    mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
                } catch (PlaybackException e) {
                    handlePlaybackException(e);
                }

                // Broadcast simple.last.fm.scrobble broadcast
                TrackItem item = mCurrentList.get(mCurrentPlayingIndex);
                Log.v(TAG, "Send to SLS: " + item);
                Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                bCast.putExtra("state", 0);
                bCast.putExtra("app-name", "Odyssey");
                bCast.putExtra("app-package", "org.odyssey");
                bCast.putExtra("artist", item.getTrackArtist());
                bCast.putExtra("album", item.getTrackAlbum());
                bCast.putExtra("track", item.getTrackTitle());
                bCast.putExtra("duration", item.getTrackDuration() / 1000);
                sendBroadcast(bCast);

                // Check if next song is availible (gapless)
                if (mNextPlayingIndex < mCurrentList.size() && (mNextPlayingIndex >= 0)) {
                    try {
                        mPlayer.setNextTrack(mCurrentList.get(mNextPlayingIndex).getTrackURL());
                    } catch (PlaybackException e) {
                        handlePlaybackException(e);
                    }
                }
            }
        } else {

            if (mCurrentPlayingIndex - 1 >= 0) {
                mCurrentPlayingIndex--;
            } else if (mRepeat == REPEATSTATE.REPEAT_ALL.ordinal()) {
                // In repeat mode next track is last track of playlist
                mCurrentPlayingIndex = mCurrentList.size() - 1;
            }

            // Next track is availible
            if (mCurrentPlayingIndex < mCurrentList.size() && mCurrentPlayingIndex >= 0) {
                // Start playback of new song
                try {
                    mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
                } catch (PlaybackException e) {
                    handlePlaybackException(e);
                }

                // Broadcast simple.last.fm.scrobble broadcast
                TrackItem item = mCurrentList.get(mCurrentPlayingIndex);
                Log.v(TAG, "Send to SLS: " + item);
                Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                bCast.putExtra("state", 0);
                bCast.putExtra("app-name", "Odyssey");
                bCast.putExtra("app-package", "org.odyssey");
                bCast.putExtra("artist", item.getTrackArtist());
                bCast.putExtra("album", item.getTrackAlbum());
                bCast.putExtra("track", item.getTrackTitle());
                bCast.putExtra("duration", item.getTrackDuration() / 1000);
                sendBroadcast(bCast);

                // Check if next song is availible (gapless)
                if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                    try {
                        mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
                    } catch (PlaybackException e) {
                        handlePlaybackException(e);
                    }
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
        mCurrentPlayingIndex = -1;
    }

    public void jumpToIndex(int index, boolean startPlayback) {
        jumpToIndex(index, startPlayback, 0);
    }

    public void jumpToIndex(int index, boolean startPlayback, int jumpTime) {
        Log.v(TAG, "Playback of index: " + index + " requested");
        Log.v(TAG, "Playlist size: " + mCurrentList.size());
        // Stop playback
        mPlayer.stop();
        // Set currentindex to new song
        if (index < mCurrentList.size() && index >= 0) {
            mCurrentPlayingIndex = index;
            Log.v(TAG, "Start playback of: " + mCurrentList.get(mCurrentPlayingIndex));

            // Broadcast simple.last.fm.scrobble broadcast
            TrackItem item = mCurrentList.get(mCurrentPlayingIndex);
            if (startPlayback) {

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
                mIsPaused = false;

                try {
                    mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL());
                } catch (PlaybackException e) {
                    handlePlaybackException(e);
                }
                Log.v(TAG, "Send to SLS: " + item);
                Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                bCast.putExtra("state", 0);
                bCast.putExtra("app-name", "Odyssey");
                bCast.putExtra("app-package", "org.odyssey");
                bCast.putExtra("artist", item.getTrackArtist());
                bCast.putExtra("album", item.getTrackAlbum());
                bCast.putExtra("track", item.getTrackTitle());
                bCast.putExtra("duration", item.getTrackDuration() / 1000);

                updateStatus();
            } else {
                try {
                    mPlayer.play(mCurrentList.get(mCurrentPlayingIndex).getTrackURL(), jumpTime);
                } catch (PlaybackException e) {
                    handlePlaybackException(e);
                }
            }

            // Check if another song follows current one for gapless
            // playback
            if ((mCurrentPlayingIndex + 1) < mCurrentList.size()) {
                Log.v(TAG, "Set next track to: " + mCurrentList.get(mCurrentPlayingIndex + 1));
                try {
                    mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
                } catch (PlaybackException e) {
                    handlePlaybackException(e);
                }
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
            } catch (PlaybackException e) {
                handlePlaybackException(e);
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
            jumpToIndex(index, true);
        } else if ((mCurrentPlayingIndex + 1) == index) {
            // Deletion of next song which requires extra handling
            // because of gapless playback, set next song to next on
            mCurrentList.remove(index);
            try {
                mPlayer.setNextTrack(mCurrentList.get(index).getTrackURL());
            } catch (PlaybackException e) {
                handlePlaybackException(e);
            }
        } else if (index >= 0 && index < mCurrentList.size()) {
            mCurrentList.remove(index);
            // mCurrentIndex is now moved one position up so set variable
            if (index < mCurrentPlayingIndex) {
                mCurrentPlayingIndex--;
            }
        }
        // Send new NowPlaying because playlist changed
        // sendUpdateBroadcast();
        updateStatus();
    }

    /**
     * Stops the gapless mediaplayer and cancels the foreground service. Removes
     * any ongoing notification.
     */
    public void stopService() {
        // save currentlist to database
        mPlaylistManager.savePlaylist(mCurrentList);

        mPlayer.stop();
        stopForeground(true);
        mNotificationBuilder.setOngoing(false);
        mNotificationManager.cancel(NOTIFICATION_ID);
        stopSelf();
    }

    public int getRandom() {
        return mRandom;
    }

    public int getRepeat() {
        return mRepeat;
    }

    public void setRepeat(int repeat) {
        mRepeat = repeat;
    }

    public void setRandom(int random) {
        // TODO set next mp to random one,too
        mRandom = random;
    }

    /**
     * Registers callback interfaces from distant processes which receive the
     * NowPlayingInformation
     * 
     * @param callback
     */
    public synchronized void registerNowPlayingCallback(IOdysseyNowPlayingCallback callback) {
        Log.v(TAG, "Added NowPlaying callback");
        // mutex lock
        try {
            mCallbackMutex.acquire();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            // Cancel (not safe)
            return;
        }
        mNowPlayingCallbacks.add(callback);
        // mutex unlock
        mCallbackMutex.release();
        // Notify about current status right away
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
            String playingURL = mCurrentList.get(mCurrentPlayingIndex).getTrackURL();
            int playing = mPlayer.isRunning() ? 1 : 0;
            try {
                // FIXME nicer
                callback.receiveNewNowPlayingInformation(new NowPlayingInformation(playing, playingURL, mCurrentPlayingIndex, mRepeat, mRandom, mCurrentList.size()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Unregister callback interfaces from distant processes
     * 
     * @param callback
     */
    public synchronized void unregisterNowPlayingCallback(IOdysseyNowPlayingCallback callback) {
        Log.v(TAG, "Unregistering callback");
        // mutex lock
        try {
            mCallbackMutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            // Cancel here
            return;
        }
        mNowPlayingCallbacks.remove(callback);
        // mutex unlock
        mCallbackMutex.release();
    }

    public int getCurrentIndex() {
        return mCurrentPlayingIndex;
    }

    public TrackItem getCurrentTrack() {
        if (mCurrentPlayingIndex >= 0 && mCurrentList.size() > mCurrentPlayingIndex) {
            return mCurrentList.get(mCurrentPlayingIndex);
        }
        return null;
    }

    /*
     * This method should be safe to call at any time. So it should check the
     * current state of gaplessplayer, playbackservice and so on.
     */
    private synchronized void updateStatus() {
        // Check if playlist contains any tracks otherwise playback should not
        // be possible
        if (mCurrentList.size() > 0) {
            // Check current playback state. If playing inform all listeners and
            // check if notification is set, and set if not.
            if (mPlayer.isRunning() && (mCurrentPlayingIndex >= 0)) {
                // Get the actual trackitem and distribute the information
                TrackItem trackItem = mCurrentList.get(mCurrentPlayingIndex);
                setLockscreenPicture(trackItem, PLAYSTATE.PLAYING);
                setNotification(trackItem, PLAYSTATE.PLAYING);
                notifyNowPlayingListeners(trackItem, PLAYSTATE.PLAYING);
                broadcastPlaybackInformation(trackItem, PLAYSTATE.PLAYING);
            } else if (mPlayer.isPaused() && (mCurrentPlayingIndex >= 0)) {
                TrackItem trackItem = mCurrentList.get(mCurrentPlayingIndex);
                setLockscreenPicture(trackItem, PLAYSTATE.PAUSE);
                setNotification(trackItem, PLAYSTATE.PAUSE);
                notifyNowPlayingListeners(trackItem, PLAYSTATE.PAUSE);
                broadcastPlaybackInformation(trackItem, PLAYSTATE.PAUSE);
            } else {
                // Remove notification if shown
                clearNotification();
                setLockscreenPicture(null, PLAYSTATE.STOPPED);
                notifyNowPlayingListeners(null, PLAYSTATE.STOPPED);
                broadcastPlaybackInformation(null, PLAYSTATE.STOPPED);
            }
        } else {
            // No playback, check if notification is set and remove it then
            clearNotification();
            setLockscreenPicture(null, PLAYSTATE.STOPPED);
            // Notify all listeners with broadcast about playing situation
            notifyNowPlayingListeners(null, PLAYSTATE.STOPPED);
            broadcastPlaybackInformation(null, PLAYSTATE.STOPPED);
        }

    }

    private void clearNotification() {
        if (mNotification != null) {
            stopForeground(true);
        }
    }

    private void setLockscreenPicture(TrackItem track, PLAYSTATE playbackState) {
        // Clear if track == null
        if (track != null) {
            // Retrieve image url from androids database
            String where = android.provider.MediaStore.Audio.Albums.ALBUM_KEY + "=?";

            String whereVal[] = { mCurrentList.get(mCurrentPlayingIndex).getTrackAlbumKey() };

            Cursor cursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART }, where, whereVal, "");

            String coverPath = null;
            if (cursor.moveToFirst()) {
                coverPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            }

            cursor.close();

            // Create drawable from url
            BitmapDrawable cover;

            RemoteControlClient.MetadataEditor editor = mRemoteControlClient.editMetadata(false);

            /*
             * Check if picture exists, otherwise set null picture which should
             * clear the last one but seems to be broken for android 4.4
             */

            if (coverPath != null) {
                if (coverPath != mLastCoverURL) {
                    mLastCoverURL = coverPath;
                    cover = (BitmapDrawable) BitmapDrawable.createFromPath(mLastCoverURL);
                    editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, cover.getBitmap());
                }

            } else {
                editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, null);
            }
            // Set other information
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, track.getTrackAlbum());
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, track.getTrackArtist());
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, track.getTrackArtist());
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, track.getTrackTitle());
            editor.apply();

            // Check playstate for buttons
            if (playbackState == PLAYSTATE.PLAYING) {
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            } else if (playbackState == PLAYSTATE.PAUSE) {
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
            }
            // Apply some flags, ex. which buttons to show
            mRemoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_NEXT | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS);
        } else {
            // Clear lockscreen
            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        }
    }

    private void setNotification(TrackItem track, PLAYSTATE playbackState) {
        if (track != null) {
            Intent resultIntent = new Intent(this, MainActivity.class);
            resultIntent.putExtra("Fragment", "currentsong");

            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            mNotificationBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_odys).setContentTitle("Odyssey").setContentText("");

            mNotificationBuilder.setContentTitle(track.getTrackTitle());
            mNotificationBuilder.setContentText(track.getTrackArtist());
            mNotificationBuilder.setSubText(track.getTrackAlbum());

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
            mNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

            mNotification = mNotificationBuilder.build();
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
            startForeground(NOTIFICATION_ID, mNotification);
        } else {
            clearNotification();
        }
    }

    private void broadcastPlaybackInformation(TrackItem track, PLAYSTATE state) {
        if (track != null) {
            // Create the broadcast intent
            Intent broadcastIntent = new Intent(MESSAGE_NEWTRACKINFORMATION);

            // TODO check if extra list is neccessary
            // Add currentTrack to parcel
            ArrayList<Parcelable> extraTrackItemList = new ArrayList<Parcelable>();
            extraTrackItemList.add(mCurrentList.get(mCurrentPlayingIndex));

            // Create NowPlayingInfo for parcel
            int playing = (state == PLAYSTATE.PLAYING ? 1 : 0);
            String playingURL = track.getTrackURL();
            int playingIndex = mCurrentPlayingIndex;
            int repeat = mRepeat;
            int random = mRandom;
            int playlistlength = mCurrentList.size();
            NowPlayingInformation info = new NowPlayingInformation(playing, playingURL, playingIndex, repeat, random, playlistlength);

            // Add nowplayingInfo to parcel
            ArrayList<Parcelable> extraNPList = new ArrayList<Parcelable>();
            extraNPList.add(info);

            // Add this stuff to the parcel
            broadcastIntent.putParcelableArrayListExtra(INTENT_TRACKITEMNAME, extraTrackItemList);
            broadcastIntent.putParcelableArrayListExtra(INTENT_NOWPLAYINGNAME, extraNPList);

            // We're good to go, send it away
            sendBroadcast(broadcastIntent);
        } else {
            // TODO fix Widget and stuff for tolerance without this information
            // Send empty broadcast with stopped information
            Intent broadcastIntent = new Intent(MESSAGE_NEWTRACKINFORMATION);

            // Add empty trackitem to parcel
            ArrayList<Parcelable> extraTrackItemList = new ArrayList<Parcelable>();
            extraTrackItemList.add(new TrackItem());

            NowPlayingInformation info = new NowPlayingInformation(0, "", -1, mRepeat, mRandom, mCurrentList.size());
            // Add nowplayingInfo to parcel
            ArrayList<Parcelable> extraNPList = new ArrayList<Parcelable>();
            extraNPList.add(info);

            // Add this stuff to the parcel
            broadcastIntent.putParcelableArrayListExtra(INTENT_TRACKITEMNAME, extraTrackItemList);
            broadcastIntent.putParcelableArrayListExtra(INTENT_NOWPLAYINGNAME, extraNPList);

            // We're good to go, send it away
            sendBroadcast(broadcastIntent);
        }
    }

    private void notifyNowPlayingListeners(TrackItem item, PLAYSTATE state) {
        if (item != null) {
            /*
             * Sends a new NowPlaying object on its way to connected callbacks
             * PlaybackService --> OdysseyApplication |-> Homescreen-widget
             */

            // Create data package for distribution
            int playing = (state == PLAYSTATE.PLAYING ? 1 : 0);
            String playingURL = item.getTrackURL();
            int playingIndex = mCurrentPlayingIndex;
            int repeat = mRepeat;
            int random = mRandom;
            int playlistlength = mCurrentList.size();

            NowPlayingInformation info = new NowPlayingInformation(playing, playingURL, playingIndex, repeat, random, playlistlength);

            try {
                mCallbackMutex.acquire();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
                // Cancel here, because it isn't safe
                return;
            }

            for (IOdysseyNowPlayingCallback callback : mNowPlayingCallbacks) {
                Log.v(TAG, "Sending now playing information to receiver");
                try {
                    callback.receiveNewNowPlayingInformation(info);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            mCallbackMutex.release();
        } else {
            NowPlayingInformation info = new NowPlayingInformation(0, "", -1, mRepeat, mRandom, mCurrentList.size());

            try {
                mCallbackMutex.acquire();
            } catch (InterruptedException e1) {
                // Cancel here, because it isn't safe
                return;
            }
            for (IOdysseyNowPlayingCallback callback : mNowPlayingCallbacks) {
                Log.v(TAG, "Sending now playing information to receiver");
                try {
                    callback.receiveNewNowPlayingInformation(info);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            mCallbackMutex.release();
        }
    }

    private void handlePlaybackException(PlaybackException exception) {
        Log.v(TAG, "Exception occured: " + exception.getReason().toString());
        Toast.makeText(getBaseContext(), TAG + ":" + exception.getReason().toString(), Toast.LENGTH_LONG).show();
        // TODO better handling?
        // Stop service on exception for now
        stop();
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
        public void setRandom(int random) throws RemoteException {
            // Create random control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RANDOM, random);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void setRepeat(int repeat) throws RemoteException {
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
        public int getRandom() throws RemoteException {
            return mService.get().getRandom();
        }

        @Override
        public int getRepeat() throws RemoteException {
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

        @Override
        public void shufflePlaylist() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SHUFFLEPLAYLIST);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void playAllTracks() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYALLTRACKS);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public int getCurrentIndex() throws RemoteException {
            return mService.get().getCurrentIndex();
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

            // broadcastNowPlaying(new NowPlayingInformation(1,
            // mCurrentList.get(mCurrentPlayingIndex).getTrackURL(),
            // mCurrentPlayingIndex, mRepeat, mRandom));
            // updateNotification();
            updateStatus();
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

            // Broadcast simple.last.fm.scrobble broadcast
            TrackItem item = mCurrentList.get(mCurrentPlayingIndex);
            Log.v(TAG, "Send to SLS: " + item);
            Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
            bCast.putExtra("state", 3);
            bCast.putExtra("app-name", "Odyssey");
            bCast.putExtra("app-package", "org.odyssey");
            bCast.putExtra("artist", item.getTrackArtist());
            bCast.putExtra("album", item.getTrackAlbum());
            bCast.putExtra("track", item.getTrackTitle());
            bCast.putExtra("duration", item.getTrackDuration() / 1000);
            sendBroadcast(bCast);

            if (mRandom == RANDOMSTATE.RANDOM_ON.ordinal()) {
                // save lastindex for previous
                mLastPlayingIndex = mCurrentPlayingIndex;

                // set currentindex to nextindex if exists
                if (mNextPlayingIndex == -1) {
                    // create new random index
                    mCurrentPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
                    // if next index equal to current index create a new random
                    // index but just trying 20 times
                    int counter = 0;
                    while (mLastPlayingIndex == mCurrentPlayingIndex && counter > 20) {
                        mCurrentPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
                        counter++;
                    }
                } else {
                    mCurrentPlayingIndex = mNextPlayingIndex;
                }

                updateStatus();

                // Broadcast simple.last.fm.scrobble broadcast
                TrackItem newTrackitem = mCurrentList.get(mCurrentPlayingIndex);
                Log.v(TAG, "Send to SLS: " + newTrackitem);
                Intent newbCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                newbCast.putExtra("state", 0);
                newbCast.putExtra("app-name", "Odyssey");
                newbCast.putExtra("app-package", "org.odyssey");
                newbCast.putExtra("artist", newTrackitem.getTrackArtist());
                newbCast.putExtra("album", newTrackitem.getTrackAlbum());
                newbCast.putExtra("track", newTrackitem.getTrackTitle());
                newbCast.putExtra("duration", newTrackitem.getTrackDuration() / 1000);
                sendBroadcast(newbCast);

                // create new random nextindex
                mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
                // if next index equal to current index create a new random
                // index but just trying 20 times
                int counter = 0;
                while (mNextPlayingIndex == mCurrentPlayingIndex && counter > 20) {
                    mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
                    counter++;
                }

                // set next track for gapless playback
                if (mNextPlayingIndex < mCurrentList.size() && (mNextPlayingIndex >= 0)) {
                    try {
                        mPlayer.setNextTrack(mCurrentList.get(mNextPlayingIndex).getTrackURL());
                    } catch (PlaybackException e) {
                        handlePlaybackException(e);
                    }
                }
            } else {

                // save lastindex for previous in random mode
                mLastPlayingIndex = mCurrentPlayingIndex;

                // Check if this song was the last one in the playlist
                if ((mCurrentPlayingIndex + 1) == mCurrentList.size()) {
                    if (mRepeat == REPEATSTATE.REPEAT_ALL.ordinal()) {
                        // Was last song in list so repeat playlist
                        jumpToIndex(0, true);
                    } else {
                        // Was last song in list stop everything
                        Log.v(TAG, "Last song played");
                        stop();
                    }
                } else {
                    // At least one song to go
                    mCurrentPlayingIndex++;

                    updateStatus();

                    // Broadcast simple.last.fm.scrobble broadcast
                    TrackItem newTrackitem = mCurrentList.get(mCurrentPlayingIndex);
                    Log.v(TAG, "Send to SLS: " + newTrackitem);
                    Intent newbCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                    newbCast.putExtra("state", 0);
                    newbCast.putExtra("app-name", "Odyssey");
                    newbCast.putExtra("app-package", "org.odyssey");
                    newbCast.putExtra("artist", newTrackitem.getTrackArtist());
                    newbCast.putExtra("album", newTrackitem.getTrackAlbum());
                    newbCast.putExtra("track", newTrackitem.getTrackTitle());
                    newbCast.putExtra("duration", newTrackitem.getTrackDuration() / 1000);
                    sendBroadcast(newbCast);

                    /*
                     * Check if we even have one more song to play if it is the
                     * case, schedule it for next playback (gapless playback)
                     */
                    if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                        try {
                            mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
                        } catch (PlaybackException e) {
                            handlePlaybackException(e);
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
            // Stop playback service
            // TODO save playlist position
            stop();
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
            Log.v(TAG, "Cancel odyssey playbackservice");
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
