package org.odyssey.fragments;

import java.util.ArrayList;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;
import org.odyssey.MusicLibraryHelper.TrackItem;

import android.app.ActionBar;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class PlaylistFragment extends Fragment {

	private static final String TAG = "PlaylistFragment";

	private PlaylistTracksAdapter mPlayListAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);		

		// create adapter for tracklist
		mPlayListAdapter = new PlaylistTracksAdapter(getActivity(), 
				R.layout.listview_playlist_item, new ArrayList<MusicLibraryHelper.TrackItem>());

		// create listview for tracklist
		ListView trackListView = (ListView) rootView.findViewById(R.id.listViewPlaylist);

		trackListView.setAdapter(mPlayListAdapter);

		trackListView.setOnItemClickListener(new OnItemClickListener() {

			// FIXME temporary just play clicked song
			@Override
			public void onItemClick(AdapterView<?> arg0, View viewItem, int position, long id) {

				// Get main application object for serice connection
				OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();

				try {
					app.getPlaybackService().jumpTo(position);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 

			}
		});

		setPlaylistTracks();

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();

	}

	private void setPlaylistTracks() {

		OdysseyApplication mainApplication = (OdysseyApplication) getActivity().getApplication();
		
		// get playlist
		ArrayList<String> playListURL = new ArrayList<String>();
		
		try {
			playListURL = (ArrayList<String>) mainApplication.getPlaybackService().getCurrentList();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// create trackitems
		ArrayList<MusicLibraryHelper.TrackItem> playListTracks = new ArrayList<MusicLibraryHelper.TrackItem>();
		
		for(int i = 0; i < playListURL.size(); i++) {
			MusicLibraryHelper.TrackItem item = 
					MusicLibraryHelper.getTrackItemFromURL(playListURL.get(i), getActivity().getContentResolver());
			
			playListTracks.add(item);
		}
		
		mPlayListAdapter.clear();

		mPlayListAdapter.addAll(playListTracks);		
	}

	private class PlaylistTracksAdapter extends ArrayAdapter<TrackItem> {

		private Context mContext;
		private LayoutInflater mInflater;
		private int mLayoutResourceId;

		public PlaylistTracksAdapter(Context context, int layoutResourceId, ArrayList<TrackItem> data) {
			super(context, layoutResourceId, data);

			mContext = context;
			mLayoutResourceId = layoutResourceId;
			mInflater = LayoutInflater.from(context);
			
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			TextView trackTitleView;
			TextView trackDurationView;
			TextView trackNumberView;
			TextView trackArtistView;

			if (convertView == null) {
				convertView = mInflater.inflate(mLayoutResourceId, null);
			}

			trackTitleView = (TextView) convertView.findViewById(R.id.textViewPlaylistTitleItem);
			trackDurationView = (TextView) convertView.findViewById(R.id.textViewPlaylistDurationItem);
			trackNumberView = (TextView) convertView.findViewById(R.id.textViewPlaylistNumberItem);
			trackArtistView = (TextView) convertView.findViewById(R.id.textViewPlaylistArtistItem);

			// set tracktitle
			TrackItem trackItem = getItem(position);

			trackTitleView.setText(trackItem.trackTitle);

			// calculate duration in minutes and seconds
			String seconds = String.valueOf((trackItem.trackDuration % 60000) / 1000);

			String minutes = String.valueOf(trackItem.trackDuration / 60000);

			if (seconds.length() == 1) {
				trackDurationView.setText(minutes + ":0" + seconds);
			} else {
				trackDurationView.setText(minutes + ":" + seconds);
			}

			// calculate track and discnumber
			if (("" + trackItem.trackNumber).length() < 4) {
				trackNumberView.setText("" + trackItem.trackNumber);
			} else {

				// TODO shall we use discnumber?
				String discNumber = ("" + trackItem.trackNumber).substring(0, 2);
				String trackNumber = ("" + trackItem.trackNumber).substring(2);

				trackNumberView.setText(trackNumber);
			}

			// set artist
			trackArtistView.setText(trackItem.trackArtist);

			return convertView;

		}

	}

}
