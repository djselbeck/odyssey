package org.odyssey.fragments;

import java.util.ArrayList;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;
import org.odyssey.playbackservice.TrackItem;

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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AlbumsTracksFragment extends Fragment {

	private static final String TAG = "AlbumsTracksFragment";

	public final static String ARG_ALBUMKEY = "albumkey";
	public final static String ARG_ALBUMTITLE = "albumtitle";
	public final static String ARG_ALBUMART = "albumart";
	public final static String ARG_ALBUMARTIST = "albumartist";

	private String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

	private String orderBy = android.provider.MediaStore.Audio.Media.TRACK;

	private String mAlbumKey = "";
	private String mAlbumTitle = "";
	private String mAlbumCoverPath = "";
	private String mAlbumArtist = "";
	private TrackListArrayAdapter mTrackListAdapter;

	private ImageView mCoverView;
	private TextView mAlbumTitleView;
	private TextView mAlbumArtistView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_albumtracks, container, false);

        // update actionbar
        final ActionBar actionBar = getActivity().getActionBar();

        actionBar.setHomeButtonEnabled(true);
        // allow backnavigation by homebutton 
        actionBar.setDisplayHomeAsUpEnabled(true);		
		
		// create listview header
		View headerView = inflater.inflate(R.layout.listview_header_item, null);

		mCoverView = (ImageView) headerView.findViewById(R.id.imageViewTracklistAlbumCover);

		mAlbumTitleView = (TextView) headerView.findViewById(R.id.textViewTracklistAlbumTitle);

		mAlbumArtistView = (TextView) headerView.findViewById(R.id.textViewTracklistArtistName);

		// create adapter for tracklist
		mTrackListAdapter = new TrackListArrayAdapter(getActivity(), R.layout.listview_tracklist_item, new ArrayList<TrackItem>());

		// create listview for tracklist
		ListView trackListView = (ListView) rootView.findViewById(R.id.listViewAlbumTrackList);

		trackListView.addHeaderView(headerView);

		trackListView.setAdapter(mTrackListAdapter);

		trackListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View viewItem, int position, long id) {
				
				// Play complete album
				OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();
				
				// Remove old tracks
				try {
					app.getPlaybackService().clearPlaylist();
				} catch (RemoteException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				// enqueue albumtracks
				for (int i = 0; i < mTrackListAdapter.getCount(); i++) {
					try {
						app.getPlaybackService().enqueueTrack(mTrackListAdapter.getItem(i));
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				// jump to selected track
				if (position > 0) {
					try {
						app.getPlaybackService().jumpTo(position-1);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
				} else {
					try {
						app.getPlaybackService().jumpTo(0);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
				}
			}
		});

		Bundle args = getArguments();

		mAlbumKey = args.getString(ARG_ALBUMKEY);
		mAlbumTitle = args.getString(ARG_ALBUMTITLE);
		mAlbumCoverPath = args.getString(ARG_ALBUMART);
		mAlbumArtist = args.getString(ARG_ALBUMARTIST);

		setAlbumInformation();

		setAlbumTracks();

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();

	}

	private void setAlbumInformation() {

		if (mAlbumCoverPath != null) {
			mCoverView.setImageDrawable(Drawable.createFromPath(mAlbumCoverPath));
		} else {
			mCoverView.setImageResource(R.drawable.coverplaceholder);
		}

		mAlbumTitleView.setText(mAlbumTitle);

		mAlbumArtistView.setText(mAlbumArtist);
	}

	private void setAlbumTracks() {

		String whereVal[] = { mAlbumKey };

		Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, orderBy);

		boolean isSampler = false;

		ArrayList<TrackItem> trackList = new ArrayList<TrackItem>();

		// get all tracks on the current album
		if (cursor.moveToFirst()) {
			do {
				String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
				long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
				int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
				String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
				String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
				String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

				TrackItem item = new TrackItem(title,artist,album,url,no,duration);
				if (!item.getTrackArtist().equals(mAlbumArtist)) {

					if (!item.getTrackArtist().contains(mAlbumArtist)) {

						// trackartist not albumartist and not contains
						// albumartist -> sampler
						isSampler = true;
					}

				}
				trackList.add(item);
			} while (cursor.moveToNext());
		}

		if (isSampler) {
			mAlbumArtistView.setText("");
		}

		mTrackListAdapter.setIsSampler(isSampler);

		mTrackListAdapter.addAll(trackList);
	}

	private class TrackListArrayAdapter extends ArrayAdapter<TrackItem> {

		private Context mContext;
		private LayoutInflater mInflater;
		private int mLayoutResourceId;
		private boolean mIsSampler;

		public TrackListArrayAdapter(Context context, int layoutResourceId, ArrayList<TrackItem> data) {
			super(context, layoutResourceId, data);

			mContext = context;
			mLayoutResourceId = layoutResourceId;
			mInflater = LayoutInflater.from(context);
			mIsSampler = false;

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

			trackTitleView = (TextView) convertView.findViewById(R.id.textViewTracklistTitleItem);
			trackDurationView = (TextView) convertView.findViewById(R.id.textViewTracklistDurationItem);
			trackNumberView = (TextView) convertView.findViewById(R.id.textViewTracklistNumberItem);
			trackArtistView = (TextView) convertView.findViewById(R.id.textViewTracklistArtistItem);

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

			// set artist if sampler or multiple artists
			if (mIsSampler || (trackItem.getTrackArtist().contains(mAlbumArtist) && !trackItem.getTrackArtist().equals(mAlbumArtist))) {
				trackArtistView.setText(trackItem.getTrackArtist());
			}

			return convertView;

		}

		public void setIsSampler(boolean sampler) {
			mIsSampler = sampler;
		}

	}

}
