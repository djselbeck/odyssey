package org.odyssey.fragments;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.MusicLibraryHelper.CoverBitmapGenerator;
import org.odyssey.NowPlayingInformation;
import org.odyssey.R;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.PlaybackService.RANDOMSTATE;
import org.odyssey.playbackservice.PlaybackService.REPEATSTATE;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class NowPlayingFragment extends Fragment implements OnSeekBarChangeListener {

    private TextView mTitleTextView;
    private TextView mAlbumTextView;
    private TextView mArtistTextView;
    private TextView mMinDuration;
    private TextView mMaxDuration;
    private ImageView mCoverImageView;
    private SeekBar mSeekBar;
    private PlaybackServiceConnection mServiceConnection;
    private Timer mRefreshTimer = null;
    private ImageButton mPlayPauseButton;
    private ImageButton mRepeatButton;
    private ImageButton mRandomButton;
    
    private String mImageURL;

    private final static String TAG = "OdysseyNowPlayingFragment";
    private NowPlayingReceiver mNowPlayingReceiver = null;

    private String mAlbum;

    private MusicLibraryHelper.CoverBitmapGenerator mCoverGenerator = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set visibility of quickcontrols
        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.GONE);

        // Set actionbar title
        getActivity().getActionBar().setTitle(R.string.nowplaying_fragment_title);

        View rootView = inflater.inflate(R.layout.fragment_now_playing, container, false);

        mTitleTextView = (TextView) rootView.findViewById(R.id.nowPlayingTitleView);

        mAlbumTextView = (TextView) rootView.findViewById(R.id.nowPlayingAlbumView);

        mArtistTextView = (TextView) rootView.findViewById(R.id.nowPlayingArtistView);

        mCoverImageView = (ImageView) rootView.findViewById(R.id.nowPlayingAlbumImageView);

        mMinDuration = (TextView) rootView.findViewById(R.id.nowPlayingMinValue);

        mMinDuration.setText("0:00");

        mMaxDuration = (TextView) rootView.findViewById(R.id.nowPlayingMaxValue);

        mSeekBar = (SeekBar) rootView.findViewById(R.id.nowPlayingSeekBar);

        mCoverGenerator = new CoverBitmapGenerator(getActivity(), new CoverReceiverClass());

        mAlbum = "";

        // indicate this fragment has its own menu
        setHasOptionsMenu(true);

        // set listener for seekbar
        mSeekBar.setOnSeekBarChangeListener(this);

        // Set up button listeners
        rootView.findViewById(R.id.nowPlayingNextButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mServiceConnection.getPBS().next();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        rootView.findViewById(R.id.nowPlayingPreviousButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    mServiceConnection.getPBS().previous();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        mPlayPauseButton = (ImageButton) rootView.findViewById(R.id.nowPlayingPlaypauseButton);

        mPlayPauseButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    mServiceConnection.getPBS().togglePause();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        rootView.findViewById(R.id.nowPlayingStopButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    mServiceConnection.getPBS().stop();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // TODO change repeat behavior to toggle track, playlist, nothing
        mRepeatButton = (ImageButton) rootView.findViewById(R.id.nowPlayingRepeatButton);

        mRepeatButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    int repeat = (mServiceConnection.getPBS().getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) ? REPEATSTATE.REPEAT_OFF.ordinal() : REPEATSTATE.REPEAT_ALL.ordinal();

                    mServiceConnection.getPBS().setRepeat(repeat);
                    if (repeat == 0) {
                        mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark));
                    } else {
                        mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark_active));
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // TODO change random behavior
        mRandomButton = (ImageButton) rootView.findViewById(R.id.nowPlayingRandomButton);

        mRandomButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    int random = (mServiceConnection.getPBS().getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) ? RANDOMSTATE.RANDOM_OFF.ordinal() : RANDOMSTATE.RANDOM_ON.ordinal();

                    mServiceConnection.getPBS().setRandom(random);
                    if (random == 0) {
                        mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark));
                    } else {
                        mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark_active));
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer.purge();
            mRefreshTimer = null;
        }
        if (mNowPlayingReceiver != null) {
            getActivity().getApplicationContext().unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }
        // mServiceConnection = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.now_playing_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case R.id.nowplaying_equalizer_item:
            Log.v(TAG, "opening equalizer");
            Intent startEqualizerIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            startActivityForResult(startEqualizerIntent, 0);
            return true;
        case R.id.nowplaying_settings_item:
            // FIXME
            return true;
        case R.id.nowplaying_about_item:
            // FIXME
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNowPlayingReceiver != null) {
            getActivity().getApplicationContext().unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }
        mNowPlayingReceiver = new NowPlayingReceiver();
        getActivity().getApplicationContext().registerReceiver(mNowPlayingReceiver, new IntentFilter(PlaybackService.MESSAGE_NEWTRACKINFORMATION));
        // get the playbackservice
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.setNotifier(new ServiceConnectionListener());
        mServiceConnection.openConnection();

    }

    private void updateStatus() {

        // get current track
        TrackItem currentTrack = null;
        try {
            currentTrack = mServiceConnection.getPBS().getCurrentSong();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (currentTrack == null) {
            currentTrack = new TrackItem();
        }
        // set tracktitle, album, artist and albumcover
        mTitleTextView.setText(currentTrack.getTrackTitle());

        mAlbumTextView.setText(currentTrack.getTrackAlbum());
        if (!currentTrack.getTrackAlbum().equals(mAlbum)) {
            mCoverImageView.setImageResource(R.drawable.coverplaceholder);
            mCoverGenerator.getImage(currentTrack);
        }
        mAlbum = currentTrack.getTrackAlbum();
        mArtistTextView.setText(currentTrack.getTrackArtist());

        // calculate duration in minutes and seconds
        String seconds = String.valueOf((currentTrack.getTrackDuration() % 60000) / 1000);

        String minutes = String.valueOf(currentTrack.getTrackDuration() / 60000);

        if (seconds.length() == 1) {
            mMaxDuration.setText(minutes + ":0" + seconds);
        } else {
            mMaxDuration.setText(minutes + ":" + seconds);
        }

        // set up seekbar
        mSeekBar.setMax((int) currentTrack.getTrackDuration());

        updateSeekBar();

        updateDurationView();

        try {
            final boolean isRandom = mServiceConnection.getPBS().getRandom() == 1 ? true : false;
            final boolean songPlaying = mServiceConnection.getPBS().getPlaying() == 1 ? true : false;
            final boolean isRepeat = mServiceConnection.getPBS().getRepeat() == 1 ? true : false;
            Activity activity = (Activity) getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update imagebuttons
                        if (songPlaying) {
                            mPlayPauseButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                        } else {
                            mPlayPauseButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                        }
                        if (isRepeat) {
                            mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark_active));
                        } else {
                            mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark));
                        }
                        if (isRandom) {
                            mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark_active));
                        } else {
                            mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark));
                        }

                    }
                });
            }

        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void updateSeekBar() {
        try {
            mSeekBar.setProgress(mServiceConnection.getPBS().getTrackPosition());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void updateDurationView() {
        // calculate duration in minutes and seconds
        String seconds = "";
        String minutes = "";
        try {
            if (mServiceConnection != null) {
                seconds = String.valueOf((mServiceConnection.getPBS().getTrackPosition() % 60000) / 1000);
                minutes = String.valueOf(mServiceConnection.getPBS().getTrackPosition() / 60000);
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (seconds.length() == 1) {
            mMinDuration.setText(minutes + ":0" + seconds);
        } else {
            mMinDuration.setText(minutes + ":" + seconds);
        }
    }

    private class RefreshTask extends TimerTask {

        @Override
        public void run() {
            Activity activity = (Activity) getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateDurationView();
                        updateSeekBar();
                    }
                });
            }

        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (fromUser) {
            try {
                mServiceConnection.getPBS().seekTo(progress);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    private class ServiceConnectionListener implements PlaybackServiceConnection.ConnectionNotifier {

        @Override
        public void onConnect() {
            Log.v(TAG, "Service connection established");
            updateStatus();
            if (mRefreshTimer != null) {
                mRefreshTimer.cancel();
                mRefreshTimer.purge();
                mRefreshTimer = null;
            }
            mRefreshTimer = new Timer();
            mRefreshTimer.scheduleAtFixedRate(new RefreshTask(), 0, 500);
        }

        @Override
        public void onDisconnect() {
            // TODO Auto-generated method stub

        }

    }

    private class NowPlayingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.MESSAGE_NEWTRACKINFORMATION)) {
                Log.v(TAG, "Received new information");
                // Extract nowplaying info
                ArrayList<NowPlayingInformation> infoArray = intent.getExtras().getParcelableArrayList(PlaybackService.INTENT_NOWPLAYINGNAME);
                if (infoArray.size() != 0) {
                    NowPlayingInformation info = infoArray.get(0);
                    final boolean songPlaying = (info.getPlaying() == 1) ? true : false;
                    final boolean isRepeat = (info.getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) ? true : false;
                    final boolean isRandom = (info.getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) ? true : false;

                    Activity activity = (Activity) getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // update imagebuttons
                                if (songPlaying) {
                                    mPlayPauseButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                                } else {
                                    mPlayPauseButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                                }
                                if (isRepeat) {
                                    mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark_active));
                                } else {
                                    mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark));
                                }
                                if (isRandom) {
                                    mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark_active));
                                } else {
                                    mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark));
                                }
                                // update views
                                updateStatus();
                            }
                        });
                    }

                }
            }
        }
    }

    private class CoverReceiverClass implements MusicLibraryHelper.CoverBitmapListener {

        @Override
        public void receiveBitmap(final BitmapDrawable bm) {
            if (bm != null) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mCoverImageView.setImageDrawable(bm);
                        }
                    });
                }
            }
        }
    }
}
