package org.odyssey.fragments;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;
import org.odyssey.playbackservice.TrackItem;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class NowPlayingFragment extends Fragment implements OnSeekBarChangeListener{

	private TextView mTitleTextView;
	private TextView mMinDuration;
	private TextView mMaxDuration;
	private ImageView mCoverImageView;
	private SeekBar mSeekBar;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_now_playing, container, false);
		
		OdysseyApplication mainApplication = (OdysseyApplication) getActivity().getApplication();
		
		TrackItem currentTrack = null;
		try {
			currentTrack = mainApplication.getPlaybackService().getCurrentSong();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(currentTrack == null) {
			currentTrack = new TrackItem();
		}
		
		// set tracktitle and albumcover
		mTitleTextView = (TextView) rootView.findViewById(R.id.nowPlayingTitleView);
		
		mTitleTextView.setText(currentTrack.getTrackTitle());
		
		String where = android.provider.MediaStore.Audio.Albums.ALBUM + "=?";	
		
		String whereVal[] = { currentTrack.getTrackAlbum() };

		Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, 
												new String[] {MediaStore.Audio.Albums.ALBUM_ART}, where, whereVal, "");
		
		String coverPath = null;
		if(cursor.moveToFirst()) {
			coverPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
		} 
		
		mCoverImageView = (ImageView) rootView.findViewById(R.id.nowPlayingAlbumImageView);
		
		if(coverPath != null) {
			Drawable tempImage = Drawable.createFromPath(coverPath);
			mCoverImageView.setImageDrawable(tempImage);
		} else {
			mCoverImageView.setImageResource(R.drawable.coverplaceholder);
		}
		
		// set min and max value
		mMinDuration = (TextView) rootView.findViewById(R.id.nowPlayingMinValue);
		
		mMinDuration.setText("0:00");
		
		mMaxDuration = (TextView) rootView.findViewById(R.id.nowPlayingMaxValue);
		// calculate duration in minutes and seconds
		String seconds = String.valueOf((currentTrack.getTrackDuration() % 60000) / 1000);

		String minutes = String.valueOf(currentTrack.getTrackDuration() / 60000);

		if (seconds.length() == 1) {
			mMaxDuration.setText(minutes + ":0" + seconds);
		} else {
			mMaxDuration.setText(minutes + ":" + seconds);
		}			
		
		// set listener for seekbar
		mSeekBar = (SeekBar) rootView.findViewById(R.id.nowPlayingSeekBar);
		
		mSeekBar.setOnSeekBarChangeListener(this);
		
		mSeekBar.setMax((int) currentTrack.getTrackDuration());
		
		try {
			mSeekBar.setProgress(mainApplication.getPlaybackService().getTrackPosition());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return rootView;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {

		OdysseyApplication mainApplication = (OdysseyApplication) getActivity().getApplication();
//		try {
//			mainApplication.getPlaybackService().seekTo(progress);
//		} catch (RemoteException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
				
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	
}
