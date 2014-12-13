package org.odyssey.fragments;

import java.util.List;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;
import org.odyssey.adapters.ArtistsAdapter;
import org.odyssey.databasemodel.ArtistModel;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnAboutSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnPlayAllSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnSettingsSelectedListener;
import org.odyssey.loader.ArtistCoverLoader;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.TrackItem;
import org.odyssey.views.GridItem;

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

public class ArtistsSectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ArtistModel>>, OnItemClickListener {

    ArtistsAdapter mAdapter;
    OnArtistSelectedListener mArtistSelectedCallback;
    OnAboutSelectedListener mAboutSelectedCallback;
    OnSettingsSelectedListener mSettingsSelectedCallback;
    OnPlayAllSelectedListener mPlayAllSelectedCallback;

    private static final String TAG = "OdysseyArtistsSectionFragment";

    private GridView mRootGrid;

    private int mLastPosition = -1;
    private int mScrollSpeed = 0;

    private boolean mLoaderInit = false;

    private PlaybackServiceConnection mServiceConnection;

    // Listener for communication via container activity
    public interface OnArtistSelectedListener {
        public void onArtistSelected(String artist, long artistID);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
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

        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);

        mRootGrid = (GridView) rootView.findViewById(R.id.artistsGridview);

        // Add progressbar for notification of ongoing action
        mRootGrid.setEmptyView(rootView.findViewById(R.id.artistsProgressbar));

        mAdapter = new ArtistsAdapter(getActivity(), mRootGrid);

        mRootGrid.setAdapter(mAdapter);

        mRootGrid.setOnItemClickListener((OnItemClickListener) this);

        // register context menu
        registerForContextMenu(mRootGrid);

        mRootGrid.setOnScrollListener(new OnScrollListener() {
            private long mLastTime = 0;
            private int mLastFirstVisibleItem = 0;
            private boolean mFloating = false;

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    mScrollSpeed = 0;
                    mAdapter.setScrollSpeed(0);
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
                // Log.v(TAG, "Scroll from : " + firstVisibleItem +
                // " with items: " + visibleItemCount + " and total items of: "
                // + totalItemCount);
                if (firstVisibleItem != mLastFirstVisibleItem) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime == mLastTime) {
                        return;
                    }
                    long timeScrollPerRow = currentTime - mLastTime;
                    mScrollSpeed = (int) (1000 / timeScrollPerRow);
                    mAdapter.setScrollSpeed(mScrollSpeed);
                    mLastFirstVisibleItem = firstVisibleItem;
                    mLastTime = currentTime;
                    // Log.v(TAG, "Scrolling with: " + mScrollSpeed +
                    // " rows per second");

                    if (mScrollSpeed < visibleItemCount) {
                        for (int i = 0; i < visibleItemCount; i++) {
                            GridItem gridItem = (GridItem) mRootGrid.getChildAt(i);
                            gridItem.startCoverImageTask();
                        }
                    }
                }

            }
        });
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
        Log.v(TAG, "Resuming");
        super.onResume();

        // Prepare loader ( start new one or reuse old)
        // if (!mLoaderInit) {
        getLoaderManager().initLoader(0, getArguments(), this);
        // mLoaderInit = true;
        // }

        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
        Log.v(TAG, "Resumed");
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
        // Save scroll position
        mLastPosition = position;

        // identify current artist

        ArtistModel artist = (ArtistModel) mAdapter.getItem(position);

        String artistName = artist.getArtistName();
        long artistID = artist.getID();

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artistName, artistID);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.artist_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.artist_context_menu_action_enqueue:
            enqueueAllAlbums(info.position);
            return true;
        case R.id.artist_context_menu_action_play:
            playAllAlbums(info.position);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    private void enqueueAllAlbums(int position) {

        // identify current artist
        ArtistModel currentArtist = (ArtistModel) mAdapter.getItem(position);

        long artistID = currentArtist.getID();

        // get all albums of the current artist
        Cursor cursorAlbums = getActivity().getContentResolver().query(MediaStore.Audio.Artists.Albums.getContentUri("external", artistID), MusicLibraryHelper.projectionAlbums, "", null, MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");

        String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

        String orderBy = android.provider.MediaStore.Audio.Media.TRACK;

        // get all albums of the current artist
        if (cursorAlbums.moveToFirst()) {
            do {
                String[] whereVal = { cursorAlbums.getString(cursorAlbums.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY)) };

                Cursor cursorTracks = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, orderBy);

                // get all tracks of the current album
                if (cursorTracks.moveToFirst()) {
                    do {
                        String title = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        long duration = cursorTracks.getLong(cursorTracks.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        int no = cursorTracks.getInt(cursorTracks.getColumnIndex(MediaStore.Audio.Media.TRACK));
                        String artistTitle = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        String album = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                        String url = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.DATA));
                        String albumKey = cursorTracks.getString(cursorTracks.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));

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

            } while (cursorAlbums.moveToNext());
        }

    }

    private void playAllAlbums(int position) {

        // Remove old tracks
        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // get and enqueue all albums of the current artist
        enqueueAllAlbums(position);

        // play album
        try {
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public Loader<List<ArtistModel>> onCreateLoader(int arg0, Bundle arg1) {
        return new ArtistCoverLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<ArtistModel>> arg0, List<ArtistModel> arg1) {
        Log.v(TAG, "On load finished");
        mAdapter.swapModel(arg1);
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
            mRootGrid.setFastScrollEnabled(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ArtistModel>> arg0) {
        mAdapter.swapModel(null);
    }

}
