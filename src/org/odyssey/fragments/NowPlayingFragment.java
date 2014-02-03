package org.odyssey.fragments;

import org.odyssey.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class NowPlayingFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_now_playing, container, false);
		
		TextView title = (TextView) rootView.findViewById(R.id.nowPlayingTitleView);
		
		title.setText(R.string.dummy_section_text);
		
		TextView minValue = (TextView) rootView.findViewById(R.id.nowPlayingMinValue);
		
		minValue.setText("00:00");
		
		TextView maxValue = (TextView) rootView.findViewById(R.id.nowPlayingMaxValue);
		
		maxValue.setText("42:42");		
		
		ImageView image = (ImageView) rootView.findViewById(R.id.nowPlayingAlbumImageView);
		
		image.setImageResource(R.drawable.coverplaceholder);
		
		return rootView;
	}
	
}
