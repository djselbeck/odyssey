package org.odyssey.fragments;

import java.util.ArrayList;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.MusicLibraryHelper.TrackItem;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;

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

		// create listview header
		View headerView = inflater.inflate(R.layout.listview_header_item, null);

		mCoverView = (ImageView) headerView.findViewById(R.id.imageViewAlbumCover);

		mAlbumTitleView = (TextView) headerView.findViewById(R.id.textViewAlbumTitle);

		mAlbumArtistView = (TextView) headerView.findViewById(R.id.textViewArtistName);

		// create adapter for tracklist
		mTrackListAdapter = new TrackListArrayAdapter(getActivity(), R.layout.listview_tracklist_item, new ArrayList<MusicLibraryHelper.TrackItem>());

		// create listview for tracklist
		ListView trackListView = (ListView) rootView.findViewById(R.id.listViewAlbumTrackList);

		trackListView.addHeaderView(headerView);

		trackListView.setAdapter(mTrackListAdapter);

		trackListView.setOnItemClickListener(new OnItemClickListener() {

			// FIXME temporary just play clicked song
			@Override
			public void onItemClick(AdapterView<?> arg0, View viewItem, int position, long id) {
				// Respect header listitem here
				if (position > 0) {
					Log.v(TAG, "Position: " + position + " pressed");
					TrackItem tmpTrackItem = mTrackListAdapter.getItem(position - 1);
					String dataPath = tmpTrackItem.trackURL;
					Log.v(TAG, "try playback of: " + dataPath);

					// Get main application object for serice connection
					OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();

					try {
						app.getPlaybackService().play(dataPath);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// Get service handler and signal playback request
				} else {
					// Play complete album
					OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();
					// Remove old tracks
					try {
						app.getPlaybackService().clearPlaylist();
					} catch (RemoteException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					for (int i = 0; i < mTrackListAdapter.getCount(); i++) {
						String dataPath = mTrackListAdapter.getItem(i).trackURL;
						try {
							Log.v(TAG, "enqueing: " + dataPath);
							app.getPlaybackService().enqueueTrack(dataPath);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
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

		ArrayList<MusicLibraryHelper.TrackItem> trackList = new ArrayList<MusicLibraryHelper.TrackItem>();

		// get all tracks on the current album
		if (cursor.moveToFirst()) {
			do {
				MusicLibraryHelper.TrackItem item = new MusicLibraryHelper.TrackItem();
				item.trackTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
				item.trackDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
				item.trackNumber = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
				item.trackArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
				item.trackURL = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

				if (!item.trackArtist.equals(mAlbumArtist)) {

					if (!item.trackArtist.contains(mAlbumArtist)) {

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

			trackTitleView = (TextView) convertView.findViewById(R.id.textViewTrackTitleItem);
			trackDurationView = (TextView) convertView.findViewById(R.id.textViewTrackDurationItem);
			trackNumberView = (TextView) convertView.findViewById(R.id.textViewTrackNumberItem);
			trackArtistView = (TextView) convertView.findViewById(R.id.textViewTrackArtistItem);

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

			// set artist if sampler or multiple artists
			if (mIsSampler || (trackItem.trackArtist.contains(mAlbumArtist) && !trackItem.trackArtist.equals(mAlbumArtist))) {
				trackArtistView.setText(trackItem.trackArtist);
			}

			return convertView;

		}

		public void setIsSampler(boolean sampler) {
			mIsSampler = sampler;
		}

	}

}
