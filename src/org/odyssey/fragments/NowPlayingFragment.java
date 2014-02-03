package org.odyssey.fragments;

import org.odyssey.NowPlayingInformation;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;
import org.odyssey.playbackservice.IOdysseyPlaybackService;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class NowPlayingFragment extends Fragment implements OnSeekBarChangeListener, OdysseyApplication.NowPlayingListener {

	private TextView mTitleTextView;
	private TextView mMinDuration;
	private TextView mMaxDuration;
	private ImageView mCoverImageView;
	private SeekBar mSeekBar;
	private IOdysseyPlaybackService mPlayer;
	private Handler seekHandler = new Handler();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_now_playing, container, false);
		
		mTitleTextView = (TextView) rootView.findViewById(R.id.nowPlayingTitleView);
		
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

		rootView.findViewById(R.id.nowPlayingPlaypauseButton).setOnClickListener(new OnClickListener() {

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
		
		//TODO change repeat behavior to toggle track, playlist, nothing
		rootView.findViewById(R.id.nowPlayingRepeatButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					mPlayer.setRepeat(!mPlayer.getRepeat());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		//TODO change shuffle behavior
		rootView.findViewById(R.id.nowPlayingShuffleButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					mPlayer.setRandom(!mPlayer.getRandom());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});	
		
		//register for playback callbacks
		OdysseyApplication mainApplication = (OdysseyApplication) getActivity().getApplication();

		mainApplication.registerNowPlayingListener(this);
		
		return rootView;
	}
	
	private void updateStatus() {
		
		// get current track
		TrackItem currentTrack = null;
		try {
			currentTrack = mPlayer.getCurrentSong();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(currentTrack == null) {
			currentTrack = new TrackItem();
		}		
		
		// set tracktitle and albumcover
		mTitleTextView.setText(currentTrack.getTrackTitle());
		
		String where = android.provider.MediaStore.Audio.Albums.ALBUM + "=?";	
		
		String whereVal[] = { currentTrack.getTrackAlbum() };

		Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, 
												new String[] {MediaStore.Audio.Albums.ALBUM_ART}, where, whereVal, "");
		
		String coverPath = null;
		if(cursor.moveToFirst()) {
			coverPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
		} 		
		
		if(coverPath != null) {
			Drawable tempImage = Drawable.createFromPath(coverPath);
			mCoverImageView.setImageDrawable(tempImage);
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
	}
	
	private void updateSeekBar() {
		try {
			mSeekBar.setProgress(mPlayer.getTrackPosition());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//FIXME add termination condition
		//seekHandler.postDelayed(seekBarRunnable, 1000);
	}
	
	//TODO improve this
	Runnable seekBarRunnable = new Runnable() {
		   
		@Override
		public void run() {
			updateSeekBar();
		}
	};
	

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {

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
		
		new Thread() {
			public void run() {
				Activity activity = (Activity) getActivity();
				if (activity != null) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// update views
							updateStatus();
						}
					});
				}
			}
		}.start();
	}
}
