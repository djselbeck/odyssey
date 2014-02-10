package org.odyssey.views;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.NowPlayingInformation;
import org.odyssey.R;
import org.odyssey.OdysseyApplication;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuickControl extends LinearLayout implements OdysseyApplication.NowPlayingListener {
    private static final String TAG = "OdysseyQuickControl";
    TextView mTitleView;
    ImageButton mPlayPauseButton;

    public QuickControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.quickcontrol_view, this, true);
        mTitleView = (TextView) findViewById(R.id.titleView);
        mPlayPauseButton = (ImageButton) findViewById(R.id.playpauseButton);

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

        findViewById(R.id.playpauseButton).setOnClickListener(new OnClickListener() {

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
    }

    @Override
    public void onNewInformation(NowPlayingInformation info) {
        Log.v(TAG, "Info: " + info);
        final boolean songPlaying = (info.getPlaying() == 1) ? true : false;
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
                    }
                });
            }
        }.start();
    }

}
