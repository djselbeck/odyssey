package org.odyssey.views;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.NowPlayingInformation;
import org.odyssey.R;
import org.odyssey.OdysseyApplication;
import org.odyssey.playbackservice.PlaybackService.RANDOMSTATE;
import org.odyssey.playbackservice.PlaybackService.REPEATSTATE;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuickControl extends LinearLayout implements OdysseyApplication.NowPlayingListener {
    private static final String TAG = "OdysseyQuickControl";
    TextView mTitleView;
    ImageButton mPlayPauseButton;
    ImageButton mRepeatButton;
    ImageButton mRandomButton;

    public QuickControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.quickcontrol_view, this, true);
        mTitleView = (TextView) findViewById(R.id.titleView);
        mPlayPauseButton = (ImageButton) findViewById(R.id.playpauseButton);
        mRepeatButton = (ImageButton) findViewById(R.id.repeatButton);
        mRandomButton = (ImageButton) findViewById(R.id.randomButton);

        // Set up button listeners
        findViewById(R.id.nextButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                OdysseyApplication app = (OdysseyApplication) ((Activity) getContext()).getApplication();
                try {
                    app.getPlaybackService().next();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.previousButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                OdysseyApplication app = (OdysseyApplication) ((Activity) getContext()).getApplication();
                try {
                    app.getPlaybackService().previous();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        mPlayPauseButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                OdysseyApplication app = (OdysseyApplication) ((Activity) getContext()).getApplication();
                try {
                    app.getPlaybackService().togglePause();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.stopButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                OdysseyApplication app = (OdysseyApplication) ((Activity) getContext()).getApplication();
                try {
                    app.getPlaybackService().stop();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        mRepeatButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                OdysseyApplication app = (OdysseyApplication) ((Activity) getContext()).getApplication();
                try {
                    int repeat = (app.getPlaybackService().getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) ? REPEATSTATE.REPEAT_OFF.ordinal() : REPEATSTATE.REPEAT_ALL.ordinal();

                    app.getPlaybackService().setRepeat(repeat);
                    if (app.getPlaybackService().getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) {
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

        mRandomButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                OdysseyApplication app = (OdysseyApplication) ((Activity) getContext()).getApplication();
                try {
                    int random = (app.getPlaybackService().getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) ? RANDOMSTATE.RANDOM_OFF.ordinal() : RANDOMSTATE.RANDOM_ON.ordinal();

                    app.getPlaybackService().setRandom(random);
                    if (app.getPlaybackService().getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) {
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
    }

    @Override
    public void onNewInformation(NowPlayingInformation info) {
        Log.v(TAG, "Info: " + info);
        final boolean songPlaying = (info.getPlaying() == 1) ? true : false;
        final boolean isRepeat = (info.getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) ? true : false;
        final boolean isRandom = (info.getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) ? true : false;
        Log.v(TAG, "Playing: " + songPlaying);
        final TrackItem trackItem = MusicLibraryHelper.getTrackItemFromURL(info.getPlayingURL(), this.getContext().getContentResolver());
        // Make sure listeners set GUI items only from GUI thread
        new Thread() {
            public void run() {
                Activity activity = (Activity) getContext();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Resources resources = getResources();
                        mTitleView.setText(trackItem.getTrackTitle() + " - " + trackItem.getTrackArtist());
                        if (songPlaying) {
                            mPlayPauseButton.setImageDrawable(resources.getDrawable(android.R.drawable.ic_media_pause));
                        } else {
                            mPlayPauseButton.setImageDrawable(resources.getDrawable(android.R.drawable.ic_media_play));
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
                    }
                });
            }
        }.start();
    }

}
