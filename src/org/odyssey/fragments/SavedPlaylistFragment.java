package org.odyssey.fragments;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.TrackItem;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SavedPlaylistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    private static final String TAG = "OdysseySavedPlaylistFragment";

    private ListView mListView = null;

    private PlaylistTracksAdapter mPlayListAdapter;

    private PlaybackServiceConnection mServiceConnection = null;

    public final static String ARG_PLAYLISTID = "playlistid";
    public final static String ARG_PLAYLISTNAME = "playlistname";

    private long mPlaylistID = -1;
    private String mPlaylistName = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set visibility of quickcontrols
        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);

        // indicate this fragment has its own menu
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);

        mPlayListAdapter = new PlaylistTracksAdapter(getActivity(), null, 0);

        // create listview for tracklist
        mListView = (ListView) rootView.findViewById(R.id.listViewPlaylist);

        mListView.setAdapter(mPlayListAdapter);

        mListView.setOnItemClickListener((OnItemClickListener) this);

        registerForContextMenu(mListView);

        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View viewItem, int position, long id) {
        // play playlist and jump to index position

        playPlaylist(position);

    }

    @Override
    public void onStart() {
        super.onStart();

        // Prepare loader ( start new one or reuse old)
        getLoaderManager().initLoader(0, getArguments(), this);

    }

    @Override
    public void onPause() {
        super.onPause();

        mServiceConnection.closeConnection();
        mServiceConnection = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mServiceConnection = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set actionbar title
        getActivity().getActionBar().setTitle(mPlaylistName);

        // Reopen service connection
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.savedplaylist_actionbar_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_playplaylist:
            playPlaylist(0);
            return true;
        case R.id.action_removeplaylist:
            removePlaylist();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class PlaylistTracksAdapter extends CursorAdapter {
        Cursor mCursor;
        LayoutInflater mInflater;

        public PlaylistTracksAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            mCursor = c;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // TODO Auto-generated method stub

        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return null;
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

            if (mCursor == null) {
                Log.v(TAG, "NO CURSOR!");
                return convertView;
            }

            trackTitleView = (TextView) convertView.findViewById(R.id.textViewPlaylistTitleItem);
            trackDurationView = (TextView) convertView.findViewById(R.id.textViewPlaylistDurationItem);
            trackNumberView = (TextView) convertView.findViewById(R.id.textViewPlaylistNumberItem);
            trackArtistView = (TextView) convertView.findViewById(R.id.textViewPlaylistArtistItem);

            // set tracktitle
            if (mCursor.moveToPosition(position)) {

                String title = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE));
                trackTitleView.setText(title);

                // calculate duration in minutes and seconds
                long duration = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.DURATION));
                String seconds = String.valueOf((duration % 60000) / 1000);

                String minutes = String.valueOf(duration / 60000);

                if (seconds.length() == 1) {
                    trackDurationView.setText(minutes + ":0" + seconds);
                } else {
                    trackDurationView.setText(minutes + ":" + seconds);
                }

                // calculate track and discnumber
                int no = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
                if (("" + no).length() < 4) {
                    trackNumberView.setText("" + no);
                } else {

                    // TODO shall we use discnumber?
                    String discNumber = ("" + no).substring(0, 2);
                    String trackNumber = ("" + no).substring(2);

                    trackNumberView.setText(trackNumber);
                }

                // set artist
                String artistTitle = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST));
                String album = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM));
                trackArtistView.setText(artistTitle + " - " + album);

                ImageView playImage = (ImageView) convertView.findViewById(R.id.imageViewPlaylistPlay);

                playImage.setVisibility(ImageView.GONE);
            }

            return convertView;
        }

        @Override
        public Cursor swapCursor(Cursor c) {
            mCursor = c;
            return super.swapCursor(c);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.savedplaylist_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.savedplaylist_context_menu_action_remove:
            removeTrackFromPlaylist(info.position);
            return true;
        case R.id.savedplaylist_context_menu_action_enqueue:
            enqueueTrack(info.position);
            return true;
        case R.id.savedplaylist_context_menu_action_enqueueasnext:
            enqueueTrackAsNext(info.position);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    private void enqueueTrack(int position) {
        // Enqueue single track

        Cursor cursorTracks = mPlayListAdapter.getCursor();

        if (cursorTracks.moveToPosition(position)) {
            // create trackitem
            String title = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE));
            long duration = cursorTracks.getLong(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.DURATION));
            int no = cursorTracks.getInt(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
            String artistTitle = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST));
            String album = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM));
            String url = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.DATA));
            String albumKey = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM_KEY));

            TrackItem item = new TrackItem(title, artistTitle, album, url, no, duration, albumKey);

            try {
                mServiceConnection.getPBS().enqueueTrack(item);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void enqueueTrackAsNext(int position) {
        // Enqueue single track

        Cursor cursorTracks = mPlayListAdapter.getCursor();

        if (cursorTracks.moveToPosition(position)) {
            // create trackitem
            String title = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE));
            long duration = cursorTracks.getLong(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.DURATION));
            int no = cursorTracks.getInt(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
            String artistTitle = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST));
            String album = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM));
            String url = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.DATA));
            String albumKey = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM_KEY));

            TrackItem item = new TrackItem(title, artistTitle, album, url, no, duration, albumKey);

            try {
                mServiceConnection.getPBS().enqueueTrackAsNext(item);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void removePlaylist() {
        // delete current playlist

        String where = MediaStore.Audio.Playlists._ID + "=?";
        String[] whereVal = { "" + mPlaylistID };

        getActivity().getContentResolver().delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal);
    }

    private void removeTrackFromPlaylist(int position) {
        // delete selected track from playlist

        Cursor cursorTracks = mPlayListAdapter.getCursor();

        if (cursorTracks.moveToPosition(position)) {

            String where = MediaStore.Audio.Playlists.Members._ID + "=?";
            String[] whereVal = { cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members._ID)) };

            getActivity().getContentResolver().delete(MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistID), where, whereVal);

        }
    }

    private void playPlaylist(int position) {
        // Remove current playlist

        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        Cursor cursorTracks = mPlayListAdapter.getCursor();

        // get all tracks of the playlist
        if (cursorTracks.moveToFirst()) {
            do {
                String title = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE));
                long duration = cursorTracks.getLong(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.DURATION));
                int no = cursorTracks.getInt(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
                String artistTitle = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST));
                String album = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM));
                String url = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.DATA));
                String albumKey = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM_KEY));

                TrackItem item = new TrackItem(title, artistTitle, album, url, no, duration, albumKey);

                // enqueue current track
                try {
                    mServiceConnection.getPBS().enqueueTrack(item);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } while (cursorTracks.moveToNext());
        }

        // play playlist
        try {
            mServiceConnection.getPBS().jumpTo(position);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // TODO Auto-generated method stub

        if (bundle != null) {

            mPlaylistID = bundle.getLong(ARG_PLAYLISTID);
            mPlaylistName = bundle.getString(ARG_PLAYLISTNAME);

            // Set actionbar title
            getActivity().getActionBar().setTitle(mPlaylistName);
        }

        return new CursorLoader(getActivity(), MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistID), MusicLibraryHelper.projectionPlaylistTracks, "", null, "");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mPlayListAdapter.swapCursor(cursor);
        Log.v(TAG, "Loader finished");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mPlayListAdapter.swapCursor(null);
    }
}
