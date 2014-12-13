package org.odyssey.fragments;

import java.util.List;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;
import org.odyssey.adapters.AlbumAdapter;
import org.odyssey.databasemodel.AlbumModel;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnAboutSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnPlayAllSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnSettingsSelectedListener;
import org.odyssey.fragments.ArtistsSectionFragment.OnArtistSelectedListener;
import org.odyssey.loader.AlbumLoader;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.TrackItem;
import org.odyssey.views.GridItem;

import android.app.ActionBar;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class AlbumsSectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<AlbumModel>>, OnItemClickListener {

    AlbumAdapter mCursorAdapter;

    // FIXME listener in new file?
    OnAlbumSelectedListener mAlbumSelectedCallback;
    OnArtistSelectedListener mArtistSelectedCallback;
    OnAboutSelectedListener mAboutSelectedCallback;
    OnSettingsSelectedListener mSettingsSelectedCallback;
    OnPlayAllSelectedListener mPlayAllSelectedCallback;

    private String mArtist = "";
    private long mArtistID = -1;

    private boolean mLoaderInit = false;

    public final static String ARG_ARTISTNAME = "artistname";
    public final static String ARG_ARTISTID = "artistid";

    // Scroll speed in Rows per second for cover loading
    private final static int MAX_SCROLLSPEED = 10;

    private static final String TAG = "AlbumsSectionFragment";

    private GridView mRootGrid;
    private int mLastPosition;
    private int mScrollSpeed = 0;

    private PlaybackServiceConnection mServiceConnection;

    // Listener for communication via container activity
    public interface OnAlbumSelectedListener {
        public void onAlbumSelected(String albumKey, String albumTitle, String albumCoverImagePath, String albumArtist);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mAlbumSelectedCallback = (OnAlbumSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAlbumSelectedListener");
        }

        try {
            mArtistSelectedCallback = (OnArtistSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArtistSelectedListener");
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

        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        mRootGrid = (GridView) rootView.findViewById(R.id.albumsGridview);
        
        // Add progressbar for notification of ongoing action
        mRootGrid.setEmptyView(rootView.findViewById(R.id.albumsProgressbar));
        
        mCursorAdapter = new AlbumAdapter(getActivity(), mRootGrid);
        
        
        mRootGrid.setAdapter(mCursorAdapter);

        mRootGrid.setOnItemClickListener((OnItemClickListener) this);

        if (getArguments() != null) {
            // update actionbar
            final ActionBar actionBar = getActivity().getActionBar();

            actionBar.setHomeButtonEnabled(true);
            // allow backnavigation by homebutton
            actionBar.setDisplayHomeAsUpEnabled(true);

            mRootGrid.setFastScrollEnabled(false);
            mRootGrid.setFastScrollAlwaysVisible(false);
        }

        mRootGrid.setOnScrollListener(new OnScrollListener() {
            private long mLastTime = 0;
            private int mLastFirstVisibleItem = 0;
            private boolean mFloating = false;

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    mScrollSpeed = 0;
                    mCursorAdapter.setScrollSpeed(0);
                    for (int i = 0; i <= mRootGrid.getLastVisiblePosition() - mRootGrid.getFirstVisiblePosition(); i++) {
                        GridItem gridItem = (GridItem) mRootGrid.getChildAt(i);
                        gridItem.startCoverImageTask();
                    }
                    mFloating = false;
                } else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    mFloating = true;
                } else {
                    mFloating = false;
                }
            }

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem != mLastFirstVisibleItem) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime == mLastTime) {
                        return;
                    }
                    long timeScrollPerRow = currentTime - mLastTime;
                    mScrollSpeed = (int) (1000 / timeScrollPerRow);
                    mCursorAdapter.setScrollSpeed(mScrollSpeed);

                    mLastFirstVisibleItem = firstVisibleItem;
                    mLastTime = currentTime;
                    ;

                    if (mScrollSpeed < visibleItemCount) {
                        for (int i = 0; i < visibleItemCount; i++) {
                            GridItem gridItem = (GridItem) mRootGrid.getChildAt(i);
                            gridItem.startCoverImageTask();
                        }
                    }
                }

            }
        });

        // register context menu
        registerForContextMenu(mRootGrid);
        mLoaderInit = true;

        Log.v(TAG, "view created");
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
            if (mArtistID == -1) {
                mPlayAllSelectedCallback.OnPlayAllSelected();
            } else {
                // if artistID exists only play album of current artist
                playAllAlbums();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        Log.v(TAG, "Resuming");
        super.onResume();

        // Set actionbar title
        if (mArtist == "") {
            getActivity().getActionBar().setTitle(R.string.app_name);
        } else {
            getActivity().getActionBar().setTitle(mArtist);
        }

        // Prepare loader ( start new one or reuse old)
        getLoaderManager().initLoader(0, getArguments(), this);

        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
        Log.v(TAG, "Resumed");

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();
        // // Destroy loader for memory reasons
        // if (mLoaderInit) {
        // getLoaderManager().destroyLoader(0);
        // mLoaderInit = false;
        // }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // Destroy loader for memory reasons
        if (mLoaderInit) {
            getLoaderManager().destroyLoader(0);
            mLoaderInit = false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Save position for later resuming
        mLastPosition = position;

        // identify current album

        AlbumModel clickedAlbum = (AlbumModel) mCursorAdapter.getItem(position);

        String albumKey = clickedAlbum.getAlbumKey();
        String albumTitle = clickedAlbum.getAlbumName();
        String imagePath = clickedAlbum.getAlbumArtURL();
        String artistTitle = clickedAlbum.getArtistName();

        // Send the event to the host activity
        mAlbumSelectedCallback.onAlbumSelected(albumKey, albumTitle, imagePath, artistTitle);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.album_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.album_context_menu_action_enqueue:
            enqueueAlbum(info.position);
            return true;
        case R.id.album_context_menu_action_play:
            playAlbum(info.position);
            return true;
        case R.id.album_context_menu_action_artist:
            showArtist(info.position);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    private void enqueueAlbum(int position) {
        // identify current album

        AlbumModel clickedAlbum = (AlbumModel) mCursorAdapter.getItem(position);
        String albumKey = clickedAlbum.getAlbumKey();
        // get and enqueue albumtracks

        String whereVal[] = { albumKey };

        String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

        String orderBy = android.provider.MediaStore.Audio.Media.TRACK;

        Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, orderBy);

        // get all tracks on the current album
        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                TrackItem item = new TrackItem(title, artist, album, url, no, duration, albumKey);

                // enqueue current track
                try {
                    mServiceConnection.getPBS().enqueueTrack(item);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } while (cursor.moveToNext());
        }
    }

    private void playAlbum(int position) {
        // Remove old tracks
        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // get and enqueue albumtracks
        enqueueAlbum(position);

        // play album
        try {
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void playAllAlbums() {

        // play all album of current artist if exists

        // Remove old tracks
        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // get and enqueue albumtracks
        for (int i = 0; i < mCursorAdapter.getCount(); i++) {
            enqueueAlbum(i);
        }

        // play album
        try {
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void showArtist(int position) {
        // identify current artist

        AlbumModel clickedAlbum = (AlbumModel) mCursorAdapter.getItem(position);
        String artistTitle = clickedAlbum.getArtistName();

        // get artist id
        String whereVal[] = { artistTitle };

        String where = android.provider.MediaStore.Audio.Artists.ARTIST + "=?";

        String orderBy = android.provider.MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE";

        Cursor artistCursor = getActivity().getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, where, whereVal, orderBy);

        artistCursor.moveToFirst();

        long artistID = artistCursor.getLong(artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artistTitle, artistID);
    }

    @Override
    public Loader<List<AlbumModel>> onCreateLoader(int arg0, Bundle bundle) {
        if (bundle == null) {

            // all albums
            Log.v(TAG, "oncreateloaderR");

            return new AlbumLoader(getActivity(), -1);

        } else {

            // only albums of artist mArtist

            mArtist = bundle.getString(ARG_ARTISTNAME);
            mArtistID = bundle.getLong(ARG_ARTISTID);

            // Set actionbar title
            getActivity().getActionBar().setTitle(mArtist);

            return new AlbumLoader(getActivity(), mArtistID);
        }
    }

    @Override
    public void onLoadFinished(Loader<List<AlbumModel>> arg0, List<AlbumModel> model) {
        Log.v(TAG, "Loader finished");
        mCursorAdapter.swapModel(model);
        // Reset old scroll position
        if (mLastPosition >= 0) {
            Log.v(TAG, "Found old scroll position and return to: " + mLastPosition);
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<AlbumModel>> arg0) {
        mCursorAdapter.swapModel(null);
    }

}
