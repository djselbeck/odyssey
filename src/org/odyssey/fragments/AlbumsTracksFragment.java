package org.odyssey.fragments;

import java.util.ArrayList;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;
import org.odyssey.fragments.ArtistsSectionFragment.OnArtistSelectedListener;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.TrackItem;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
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

    // FIXME listener in new file?
    OnArtistSelectedListener mArtistSelectedCallback;

    private PlaybackServiceConnection mServiceConnection;

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mArtistSelectedCallback = (OnArtistSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArtistSelectedListener");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_albumtracks, container, false);

        // set visibility of quickcontrols
        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);

        // indicate this fragment has its own menu
        setHasOptionsMenu(true);

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

                playAlbum(position);

            }
        });

        Bundle args = getArguments();

        mAlbumKey = args.getString(ARG_ALBUMKEY);
        mAlbumTitle = args.getString(ARG_ALBUMTITLE);
        mAlbumCoverPath = args.getString(ARG_ALBUMART);
        mAlbumArtist = args.getString(ARG_ALBUMARTIST);

        // Set actionbar title
        getActivity().getActionBar().setTitle(mAlbumTitle);

        setAlbumInformation();

        setAlbumTracks();

        // register context menu
        registerForContextMenu(trackListView);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
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

                TrackItem item = new TrackItem(title, artist, album, url, no, duration, mAlbumKey);

                trackList.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();

        mTrackListAdapter.addAll(trackList);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.albumtracks_actionbar_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case R.id.action_playalbum:
            playAlbum(0);
            return true;
        case R.id.action_addalbum:
            enqueueAlbum();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class TrackListArrayAdapter extends ArrayAdapter<TrackItem> {

        private Context mContext;
        private LayoutInflater mInflater;
        private int mLayoutResourceId;

        public TrackListArrayAdapter(Context context, int layoutResourceId, ArrayList<TrackItem> data) {
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

            // set artist
            trackArtistView.setText(trackItem.getTrackArtist());

            return convertView;

        }
    }

    private void playAlbum(int position) {
        // clear playlist and play current album
        int index = position;

        // respect head element
        if (index > 0) {
            index = index - 1;
        }

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueueAlbum();
            mServiceConnection.getPBS().jumpTo(index);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void enqueueAlbum() {
        // Enqueue complete album

        // enqueue albumtracks
        for (int i = 0; i < mTrackListAdapter.getCount(); i++) {
            try {
                mServiceConnection.getPBS().enqueueTrack(mTrackListAdapter.getItem(i));
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void enqueueTrack(int position) {
        // Enqueue single track

        try {
            mServiceConnection.getPBS().enqueueTrack(mTrackListAdapter.getItem(position));
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void enqueueTrackAsNext(int position) {
        // Enqueue single track

        try {
            mServiceConnection.getPBS().enqueueTrackAsNext(mTrackListAdapter.getItem(position));
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void showArtist() {

        // get artist id
        String whereVal[] = { mAlbumArtist };

        String where = android.provider.MediaStore.Audio.Artists.ARTIST + "=?";

        String orderBy = android.provider.MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE";

        Cursor artistCursor = getActivity().getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, where, whereVal, orderBy);

        artistCursor.moveToFirst();

        long artistID = artistCursor.getLong(artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));

        artistCursor.close();

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(mAlbumArtist, artistID);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.album_tracks_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.album_tracks_context_menu_action_enqueue:
            if (info.position == 0) {
                enqueueAlbum();
            } else {
                // respect head element
                enqueueTrack(info.position - 1);
            }
            return true;
        case R.id.album_tracks_context_menu_action_enqueueasnext:
            if (info.position > 0) {
                enqueueTrackAsNext(info.position - 1);
            }
            return true;
        case R.id.album_tracks_context_menu_action_play:
            playAlbum(info.position);
            return true;
        case R.id.album_tracks_context_menu_action_artist:
            showArtist();
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

}
