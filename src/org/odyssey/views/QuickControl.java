package org.odyssey.views;

import org.odyssey.R;
import org.odyssey.playbackservice.PlaybackService.RANDOMSTATE;
import org.odyssey.playbackservice.PlaybackService.REPEATSTATE;
import org.odyssey.playbackservice.PlaybackServiceConnection;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuickControl extends LinearLayout {
    private static final String TAG = "OdysseyQuickControl";
    TextView mTitleView;
    ImageButton mPlayPauseButton;
    ImageButton mRepeatButton;
    ImageButton mRandomButton;

    Activity mActivity;

    private PlaybackServiceConnection mServiceConnection;

    public QuickControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.quickcontrol_view, this, true);
        mTitleView = (TextView) findViewById(R.id.titleView);
        mPlayPauseButton = (ImageButton) findViewById(R.id.playpauseButton);
        mRepeatButton = (ImageButton) findViewById(R.id.repeatButton);
        mRandomButton = (ImageButton) findViewById(R.id.randomButton);

        mServiceConnection = new PlaybackServiceConnection(context);
        mServiceConnection.openConnection();

        // Set up button listeners
        findViewById(R.id.nextButton).setOnClickListener(new OnClickListener() {
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

        findViewById(R.id.previousButton).setOnClickListener(new OnClickListener() {

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

        findViewById(R.id.stopButton).setOnClickListener(new OnClickListener() {

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

        mRepeatButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    int repeat = mServiceConnection.getPBS().getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal() ? REPEATSTATE.REPEAT_OFF.ordinal() : REPEATSTATE.REPEAT_ALL.ordinal();

                    mServiceConnection.getPBS().setRepeat(repeat);
                    if (mServiceConnection.getPBS().getRepeat() == REPEATSTATE.REPEAT_ALL.ordinal()) {
                        mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_white));
                    } else {
                        mRepeatButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat_dark));
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
                try {
                    int random = (mServiceConnection.getPBS().getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) ? RANDOMSTATE.RANDOM_OFF.ordinal() : RANDOMSTATE.RANDOM_ON.ordinal();

                    mServiceConnection.getPBS().setRandom(random);
                    if (mServiceConnection.getPBS().getRandom() == RANDOMSTATE.RANDOM_ON.ordinal()) {
                        mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_white));
                    } else {
                        mRandomButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_shuffle_dark));
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    public void setRandomButtonDrawable(Drawable drawable) {
        mRandomButton.setImageDrawable(drawable);
    }

    public void setRepeatButtonDrawable(Drawable drawable) {
        mRepeatButton.setImageDrawable(drawable);
    }

    public void setPlayPauseButtonDrawable(Drawable drawable) {
        mPlayPauseButton.setImageDrawable(drawable);
    }

    public void setText(String text) {
        mTitleView.setText(text);
    }

}
