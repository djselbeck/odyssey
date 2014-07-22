package org.odyssey.fragments;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnAboutSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnPlayAllSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnSettingsSelectedListener;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AllPlaylistsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    PlaylistsCursorAdapter mCursorAdapter;
    OnPlaylistSelectedListener mPlaylistSelectedCallback;
    OnAboutSelectedListener mAboutSelectedCallback;
    OnSettingsSelectedListener mSettingsSelectedCallback;
    OnPlayAllSelectedListener mPlayAllSelectedCallback;
    ListView mListView;

    private int mLastPosition = -1;

    private PlaybackServiceConnection mServiceConnection;

    private static final String TAG = "OdysseyAllPlaylistsFragment";

    // Listener for communication via container activity
    public interface OnPlaylistSelectedListener {
        public void onPlaylistSelected(String playlistName, long playlistID);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mPlaylistSelectedCallback = (OnPlaylistSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPlaylistSelectedListener");
        }

        try {
            mAboutSelectedCallback = (OnAboutSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAlbumSelectedListener");
        }

        try {
            mSettingsSelectedCallback = (OnSettingsSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSettingsSelectedListener");
        }

        try {
            mPlayAllSelectedCallback = (OnPlayAllSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPlayAllSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // indicate this fragment has its own menu
        setHasOptionsMenu(true);

        // set visibility of quickcontrols
        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);

        View rootView = inflater.inflate(R.layout.fragment_allplaylists, container, false);

        mCursorAdapter = new PlaylistsCursorAdapter(getActivity(), null, 0);

        mListView = (ListView) rootView.findViewById(R.id.listViewAllPlaylists);
        mListView.setAdapter(mCursorAdapter);
        mListView.setOnItemClickListener((OnItemClickListener) this);

        // Set actionbar title
        getActivity().getActionBar().setTitle(R.string.section_title_allplaylists);

        // register context menu
        registerForContextMenu(mListView);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.main, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case R.id.action_settings:
            mSettingsSelectedCallback.onSettingsSelected();
            return true;
        case R.id.action_about:
            mAboutSelectedCallback.onAboutSelected();
            return true;
        case R.id.action_playall:
            mPlayAllSelectedCallback.OnPlayAllSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLastPosition >= 0) {
            mListView.setSelection(mLastPosition);
            mLastPosition = -1;
        }
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Prepare loader ( start new one or reuse old)
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // TODO Auto-generated method stub

        return new CursorLoader(getActivity(), MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionPlaylists, "", null, MediaStore.Audio.Playlists.NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursorAdapter.swapCursor(cursor);
        Log.v(TAG, "Loader finished");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private class PlaylistsCursorAdapter extends CursorAdapter {
        Cursor mCursor;
        LayoutInflater mInflater;

        public PlaylistsCursorAdapter(Context context, Cursor c, int flags) {
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

            convertView = mInflater.inflate(R.layout.listview_allplaylists_item, null);
            if (mCursor == null) {
                Log.v(TAG, "NO CURSOR!");
                return convertView;
            }
            int nameIndex = mCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            mCursor.moveToPosition(position);
            if (nameIndex >= 0) {
                String plName = mCursor.getString(nameIndex);
                TextView textView = (TextView) convertView.findViewById(R.id.textViewAllPlaylistsName);
                textView.setText(plName);
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // Save scroll position
        mLastPosition = position;

        // identify current artist
        Cursor cursor = mCursorAdapter.getCursor();

        cursor.moveToPosition(position);

        String playlistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME));
        long playlistID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));

        // Send the event to the host activity
        mPlaylistSelectedCallback.onPlaylistSelected(playlistName, playlistID);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.all_playlist_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.saved_playlist_context_menu_action_play:
            playPlaylist(info.position);
            return true;
        case R.id.saved_playlist_context_menu_action_delete:
            deletePlaylist(info.position);
            return true;
        default:
            return super.onContextItemSelected(item);
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

        // identify current playlist
        Cursor cursorPlaylist = mCursorAdapter.getCursor();

        cursorPlaylist.moveToPosition(position);

        long playlistID = cursorPlaylist.getLong(cursorPlaylist.getColumnIndex(MediaStore.Audio.Playlists._ID));

        Cursor cursorTracks = getActivity().getContentResolver().query(MediaStore.Audio.Playlists.Members.getContentUri("external", playlistID), MusicLibraryHelper.projectionPlaylistTracks, "", null, "");

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
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void deletePlaylist(int position) {

        // identify current playlist
        Cursor cursorPlaylist = mCursorAdapter.getCursor();

        cursorPlaylist.moveToPosition(position);

        // delete current playlist
        String where = MediaStore.Audio.Playlists._ID + "=?";
        String[] whereVal = { cursorPlaylist.getString(cursorPlaylist.getColumnIndex(MediaStore.Audio.Playlists._ID)) };

        getActivity().getContentResolver().delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal);
    }
}
