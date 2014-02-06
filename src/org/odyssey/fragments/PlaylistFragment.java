package org.odyssey.fragments;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.zip.Inflater;

import org.odyssey.MainActivity;
import org.odyssey.NowPlayingInformation;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;
import org.odyssey.playbackservice.IOdysseyPlaybackService;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.webkit.WebView.FindListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PlaylistFragment extends Fragment implements OdysseyApplication.NowPlayingListener {

	private static final String TAG = "OdysseyPlaylistFragment";
	private int mPlayingIndex = 0;
	private ListView mListView = null;

	private PlaylistTracksAdapter mPlayListAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);
		
		// indicate this fragment has its own menu
		setHasOptionsMenu(true);
		
		View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);

		// create adapter for tracklist
		mPlayListAdapter = new PlaylistTracksAdapter(((OdysseyApplication)getActivity().getApplication()).getPlaybackService());

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

		OdysseyApplication mainApplication = (OdysseyApplication) getActivity().getApplication();

		mainApplication.registerNowPlayingListener(this);
		
		// register context menu
		registerForContextMenu(mListView);		

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
				mPlayListAdapter.clear();

				mPlayListAdapter.notifyDataSetChanged();
				return true;
	        case R.id.action_jumpcurrent:
	        	mListView.setSelection(mPlayingIndex);
	    }
	    return super.onOptionsItemSelected(item);
	}	



	private class PlaylistTracksAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private int mAdapterPlayingIndex = 0;
		private WeakReference<IOdysseyPlaybackService> mPlaybackService;

		public PlaylistTracksAdapter(IOdysseyPlaybackService iOdysseyPlaybackService) {
			super();

			mInflater = getLayoutInflater(getArguments());
			mPlaybackService = new WeakReference<IOdysseyPlaybackService>(iOdysseyPlaybackService);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			TextView trackTitleView;
			TextView trackDurationView;
			TextView trackNumberView;
			TextView trackArtistView;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.listview_playlist_item, null);
			}

			trackTitleView = (TextView) convertView.findViewById(R.id.textViewPlaylistTitleItem);
			trackDurationView = (TextView) convertView.findViewById(R.id.textViewPlaylistDurationItem);
			trackNumberView = (TextView) convertView.findViewById(R.id.textViewPlaylistNumberItem);
			trackArtistView = (TextView) convertView.findViewById(R.id.textViewPlaylistArtistItem);

			// set tracktitle
			TrackItem trackItem;
			try {
				trackItem = mPlaybackService.get().getPlaylistSong(position);
			} catch (RemoteException e) {
				trackItem = new TrackItem();
			}

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

			if (position == mAdapterPlayingIndex) {
				ImageView playImage = (ImageView) convertView.findViewById(R.id.imageViewPlaylistPlay);

				playImage.setVisibility(ImageView.VISIBLE);
			} else {
				ImageView playImage = (ImageView) convertView.findViewById(R.id.imageViewPlaylistPlay);

				playImage.setVisibility(ImageView.GONE);
			}

			return convertView;

		}

		public void setPlayingIndex(int index) {
			Log.v(TAG,"Set adapter index to: " + index);
			mAdapterPlayingIndex = index;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			try {
				return mPlaybackService.get().getPlaylistSize();
			} catch (RemoteException e) {
				return 0;
			}
		}

		@Override
		public Object getItem(int position) {
			try {
				return mPlaybackService.get().getPlaylistSong(position);
			} catch (RemoteException e) {
				return null;
			}
		}

		@Override
		public long getItemId(int position) {
			//FIXME for now just return positon as id
			return position;
		}
		
		public void remove(int position) {
			try {
				mPlaybackService.get().dequeueTrackIndex(position);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			notifyDataSetChanged();
		}

		public void clear() {
			try {
				mPlaybackService.get().clearPlaylist();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			notifyDataSetChanged();
		}

	}

	@Override
	public void onNewInformation(NowPlayingInformation info) {
		mPlayingIndex = info.getPlayingIndex();		
		new Thread() {
			public void run() {
				Activity activity = (Activity) getActivity();
				if (activity != null) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mPlayListAdapter.setPlayingIndex(mPlayingIndex);
						}
					});
				}
			}
		}.start();

	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    MenuInflater inflater = getActivity().getMenuInflater();
	    inflater.inflate(R.menu.playlist_context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {	
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		if(info == null) {
			return super.onContextItemSelected(item);
		}
		
		switch (item.getItemId()) {
		case R.id.playlist_context_menu_action_remove:
			OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();
			mPlayListAdapter.remove(info.position);

			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}	
	

}
