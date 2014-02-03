package org.odyssey.fragments;

import java.util.ArrayList;
import java.util.zip.Inflater;

import org.odyssey.NowPlayingInformation;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistFragment extends Fragment implements OdysseyApplication.NowPlayingListener {

	private static final String TAG = "OdysseyPlaylistFragment";
	private int mPlayingIndex = 0;
	private ListView mListView = null;

	private PlaylistTracksAdapter mPlayListAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		// indicate this fragment has its own menu
		setHasOptionsMenu(true);
		
		View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);

		// create adapter for tracklist
		mPlayListAdapter = new PlaylistTracksAdapter(getActivity(), R.layout.listview_playlist_item, new ArrayList<TrackItem>());

		// create listview for tracklist
		mListView = (ListView) rootView.findViewById(R.id.listViewPlaylist);

		mListView.setAdapter(mPlayListAdapter);

		mListView.setOnItemClickListener(new OnItemClickListener() {

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

		OdysseyApplication mainApplication = (OdysseyApplication) getActivity().getApplication();

		mainApplication.registerNowPlayingListener(this);

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();

	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.playlist_actionbar_menu, menu);
		
		super.onCreateOptionsMenu(menu, inflater);
	}	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

	    switch (item.getItemId()) {
	        case R.id.action_clearplaylist:
				OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();
				try {
					app.getPlaybackService().clearPlaylist();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mPlayListAdapter.clear();

				mPlayListAdapter.notifyDataSetChanged();
				return true;
	        case R.id.action_jumpcurrent:
	        	mListView.setSelection(mPlayingIndex);
	    }
	    return super.onOptionsItemSelected(item);
	}	

	private void setPlaylistTracks() {

		OdysseyApplication mainApplication = (OdysseyApplication) getActivity().getApplication();

		// get playlist
		ArrayList<TrackItem> playListTracks = new ArrayList<TrackItem>();

		try {
			mainApplication.getPlaybackService().getCurrentList(playListTracks);
			for (TrackItem trackItem : playListTracks) {
				Log.v(TAG, "received track:" + trackItem);
			}
		} catch (RemoteException e) {
			Log.e(TAG, "Remote errror: " + e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mPlayListAdapter.clear();

		mPlayListAdapter.addAll(playListTracks);

		mPlayListAdapter.notifyDataSetChanged();
	}

	private class PlaylistTracksAdapter extends ArrayAdapter<TrackItem> {

		private Context mContext;
		private LayoutInflater mInflater;
		private int mLayoutResourceId;
		private int mPlayingIndex;

		public PlaylistTracksAdapter(Context context, int layoutResourceId, ArrayList<TrackItem> data) {
			super(context, layoutResourceId, data);

			mContext = context;
			mLayoutResourceId = layoutResourceId;
			mInflater = LayoutInflater.from(context);
			mPlayingIndex = 0;
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

			trackTitleView.setText(trackItem.getTrackTitle());

			// calculate duration in minutes and seconds
			String seconds = String.valueOf((trackItem.getTrackDuration() % 60000) / 1000);

			String minutes = String.valueOf(trackItem.getTrackDuration() / 60000);

			if (seconds.length() == 1) {
				trackDurationView.setText(minutes + ":0" + seconds);
			} else {
				trackDurationView.setText(minutes + ":" + seconds);
			}

			// calculate track and discnumber
			if (("" + trackItem.getTrackNumber()).length() < 4) {
				trackNumberView.setText("" + trackItem.getTrackNumber());
			} else {

				// TODO shall we use discnumber?
				String discNumber = ("" + trackItem.getTrackNumber()).substring(0, 2);
				String trackNumber = ("" + trackItem.getTrackNumber()).substring(2);

				trackNumberView.setText(trackNumber);
			}

			// set artist
			trackArtistView.setText(trackItem.getTrackArtist() + " - " + trackItem.getTrackAlbum());

			if (position == mPlayingIndex) {
				ImageView playImage = (ImageView) convertView.findViewById(R.id.imageViewPlaylistPlay);

				playImage.setVisibility(ImageView.VISIBLE);
			} else {
				ImageView playImage = (ImageView) convertView.findViewById(R.id.imageViewPlaylistPlay);

				playImage.setVisibility(ImageView.GONE);
			}

			return convertView;

		}

		public void setPlayingIndex(int index) {
			mPlayingIndex = index;
		}

	}

	@Override
	public void onNewInformation(NowPlayingInformation info) {

		final int index = info.getPlayingIndex();
		mPlayingIndex = info.getPlayingIndex();

		new Thread() {
			public void run() {
				Activity activity = (Activity) getActivity();
				if (activity != null) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mPlayListAdapter.setPlayingIndex(index);
							mPlayListAdapter.notifyDataSetChanged();
						}
					});
				}
			}
		}.start();

	}

}
