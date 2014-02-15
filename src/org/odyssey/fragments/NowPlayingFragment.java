package org.odyssey.fragments;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import org.odyssey.MainActivity;
import org.odyssey.NowPlayingInformation;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;
import org.odyssey.manager.AsyncLoader;
import org.odyssey.playbackservice.IOdysseyPlaybackService;
import org.odyssey.playbackservice.PlaybackService.RANDOMSTATE;
import org.odyssey.playbackservice.PlaybackService.REPEATSTATE;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class NowPlayingFragment extends Fragment implements OnSeekBarChangeListener, OdysseyApplication.NowPlayingListener {

    private TextView mTitleTextView;
    private TextView mAlbumTextView;
    private TextView mArtistTextView;
    private TextView mMinDuration;
    private TextView mMaxDuration;
    private ImageView mCoverImageView;
    private SeekBar mSeekBar;
    private IOdysseyPlaybackService mPlayer;
    private Timer mRefreshTimer = null;
    private ImageButton mPlayPauseButton;
    private ImageButton mRepeatButton;
    private ImageButton mRandomButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set visibility of quickcontrols
        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.GONE);

        View rootView = inflater.inflate(R.layout.fragment_now_playing, container, false);

        mTitleTextView = (TextView) rootView.findViewById(R.id.nowPlayingTitleView);

        mAlbumTextView = (TextView) rootView.findViewById(R.id.nowPlayingAlbumView);

        mArtistTextView = (TextView) rootView.findViewById(R.id.nowPlayingArtistView);

        mCoverImageView = (ImageView) rootView.findViewById(R.id.nowPlayingAlbumImageView);

        mMinDuration = (TextView) rootView.findViewById(R.id.nowPlayingMinValue);

        mMinDuration.setText("0:00");

        mMaxDuration = (TextView) rootView.findViewById(R.id.nowPlayingMaxValue);

        mSeekBar = (SeekBar) rootView.findViewById(R.id.nowPlayingSeekBar);

        // set listener for seekbar
        mSeekBar.setOnSeekBarChangeListener(this);

        // get the playbackservice
        mPlayer = ((OdysseyApplication) getActivity().getApplication()).getPlaybackService();

        // Set up button listeners
        rootView.findViewById(R.id.nowPlayingNextButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mPlayer.next();
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
                    mPlayer.previous();
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
                    mPlayer.togglePause();
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
                    mPlayer.stop();
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
                    int repeat = (mPlayer.getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) ? REPEATSTATE.REPEAT_OFF.ordinal() : REPEATSTATE.REPEAT_ALL.ordinal();

                    mPlayer.setRepeat(repeat);
                    if (mPlayer.getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) {
                        mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_white));
                    } else {
                        mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat));
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
                    int random = (mPlayer.getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) ? RANDOMSTATE.RANDOM_OFF.ordinal() : RANDOMSTATE.RANDOM_ON.ordinal();

                    mPlayer.setRandom(random);
                    if (mPlayer.getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) {
                        mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_white));
                    } else {
                        mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle));
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // register for playback callbacks
        OdysseyApplication mainApplication = (OdysseyApplication) getActivity().getApplication();

        mainApplication.registerNowPlayingListener(this);

        // Create timer for seekbar refresh
        mRefreshTimer = new Timer();

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        mRefreshTimer.cancel();
        mRefreshTimer = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // get the playbackservice
        mPlayer = ((OdysseyApplication) getActivity().getApplication()).getPlaybackService();
        mRefreshTimer = new Timer();
        mRefreshTimer.scheduleAtFixedRate(new RefreshTask(), 0, 500);
    }

    private void updateStatus() {
        mPlayer = ((OdysseyApplication) getActivity().getApplication()).getPlaybackService();

        // get current track
        TrackItem currentTrack = null;
        try {
            currentTrack = mPlayer.getCurrentSong();
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

        mArtistTextView.setText(currentTrack.getTrackArtist());

        String where = android.provider.MediaStore.Audio.Albums.ALBUM_KEY + "=?";

        String whereVal[] = { currentTrack.getTrackAlbumKey() };

        Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART }, where, whereVal, "");

        String coverPath = null;
        if (cursor.moveToFirst()) {
            coverPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        }

        if (coverPath != null) {
            // create and execute new asynctask
            AsyncLoader.CoverViewHolder coverHolder = new AsyncLoader.CoverViewHolder();
            coverHolder.coverViewReference = new WeakReference<ImageView>(mCoverImageView);
            coverHolder.imagePath = coverPath;
            coverHolder.task = new AsyncLoader();

            coverHolder.task.execute(coverHolder);
        } else {
            mCoverImageView.setImageResource(R.drawable.coverplaceholder);
        }

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
    }

    private void updateSeekBar() {
        IOdysseyPlaybackService service = ((OdysseyApplication) getActivity().getApplication()).getPlaybackService();
        try {
            mSeekBar.setProgress(service.getTrackPosition());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void updateDurationView() {
        // calculate duration in minutes and seconds
        String seconds = "";
        String minutes = "";
        IOdysseyPlaybackService service = ((OdysseyApplication) getActivity().getApplication()).getPlaybackService();
        try {
            seconds = String.valueOf((service.getTrackPosition() % 60000) / 1000);
            minutes = String.valueOf(service.getTrackPosition() / 60000);
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
                mPlayer.seekTo(progress);
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

    @Override
    public void onNewInformation(NowPlayingInformation info) {

        final boolean songPlaying = (info.getPlaying() == 1) ? true : false;
        final boolean isRepeat = (info.getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) ? true : false;
        final boolean isRandom = (info.getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) ? true : false;

        new Thread() {
            public void run() {
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
                                mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_white));
                            } else {
                                mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat));
                            }
                            if (isRandom) {
                                mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_white));
                            } else {
                                mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle));
                            }
                            // update views
                            updateStatus();
                        }
                    });
                }
            }
        }.start();
    }
}
