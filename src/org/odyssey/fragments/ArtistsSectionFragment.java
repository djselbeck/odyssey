package org.odyssey.fragments;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnAboutSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnPlayAllSelectedListener;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment.OnSettingsSelectedListener;
import org.odyssey.manager.ArtistCoverLoader;
import org.odyssey.manager.AsyncLoader;
import org.odyssey.manager.AsyncLoader.CoverViewHolder;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
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
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class ArtistsSectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    ArtistsCursorAdapter mCursorAdapter;
    OnArtistSelectedListener mArtistSelectedCallback;
    OnAboutSelectedListener mAboutSelectedCallback;
    OnSettingsSelectedListener mSettingsSelectedCallback;
    OnPlayAllSelectedListener mPlayAllSelectedCallback;

    private static final String TAG = "ArtistsSectionFragment";

    private GridView mRootGrid;

    private int mLastPosition = -1;

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

        mCursorAdapter = new ArtistsCursorAdapter(getActivity(), null, 0);

        mRootGrid = (GridView) rootView;

        mRootGrid.setAdapter(mCursorAdapter);

        mRootGrid.setOnItemClickListener((OnItemClickListener) this);

        // Prepare loader ( start new one or reuse old)
        getLoaderManager().initLoader(0, null, this);

        // register context menu
        registerForContextMenu(mRootGrid);

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
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
            mRootGrid.setFastScrollEnabled(true);
        }
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
        Log.v(TAG, "Resumed");
    }

    private class ArtistsCursorAdapter extends CursorAdapter implements SectionIndexer {

        private LayoutInflater mInflater;
        private Cursor mCursor;
        private LruCache<String, Bitmap> mCache;
        ArrayList<String> mSectionList;
        ArrayList<Integer> mSectionPositions;

        public ArtistsCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);

            this.mInflater = LayoutInflater.from(context);
            this.mCursor = c;
            this.mCache = new LruCache<String, Bitmap>(24);
            mSectionList = new ArrayList<String>();
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // placeholder
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {

            // placeholder
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            int coverIndex = -1;
            int labelIndex = -1;

            AsyncLoader.CoverViewHolder coverHolder = null;

            if (true) {

                convertView = mInflater.inflate(R.layout.item_artists, null);
                convertView.setLayoutParams(new LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
                // create new coverholder for imageview(cover) and
                // textview(artistlabel)
                coverHolder = new AsyncLoader.CoverViewHolder();
                coverHolder.coverViewReference = new WeakReference<ImageView>((ImageView) convertView.findViewById(R.id.imageViewArtists));
                coverHolder.coverViewSwitcher = new WeakReference<ViewSwitcher>((ViewSwitcher) convertView.findViewById(R.id.albumgridSwitcher));
                coverHolder.labelView = (TextView) convertView.findViewById(R.id.textViewArtistsItem);

                convertView.setTag(coverHolder);

            } else {
                // get coverholder from convertview and cancel asynctask
                coverHolder = (CoverViewHolder) convertView.getTag();
                if (coverHolder.task != null)
                    coverHolder.task.cancel(true);
            }

            // get imagepath and labeltext
            if (this.mCursor == null) {
                convertView.setLayoutParams(new LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
                return convertView;
            }

            this.mCursor.moveToPosition(position);

            coverIndex = mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
            labelIndex = mCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);

            if (labelIndex >= 0) {
                // coverHolder.labelText = mCursor.getString(labelIndex);
                String labelText = mCursor.getString(labelIndex);
                if (labelText != null) {
                    coverHolder.labelView.setText(labelText);
                }
            } else {
                // placeholder for empty labels
                coverHolder.labelView.setText("");
            }

            // Check for valid column
            if (coverIndex >= 0) {
                // Get column value (Image-URL)
                coverHolder.imagePath = mCursor.getString(coverIndex);
                if (coverHolder.imagePath != null) {
                    // Check cache first
                    Bitmap cacheImage = mCache.get(coverHolder.imagePath);
                    if (cacheImage == null) {
                        // Cache miss
                        // create and execute new asynctask
                        coverHolder.task = new AsyncLoader();
                        coverHolder.cache = new WeakReference<LruCache<String, Bitmap>>(mCache);
                        coverHolder.task.execute(coverHolder);
                    } else {
                        // Cache hit
                        coverHolder.coverViewReference.get().setImageBitmap(cacheImage);
                        coverHolder.coverViewSwitcher.get().setDisplayedChild(1);
                    }
                }
            } else {
                coverHolder.imagePath = null;
            }

            return convertView;
        }

        @Override
        public Cursor swapCursor(Cursor c) {

            this.mCursor = c;

            if (mCursor == null) {
                return super.swapCursor(c);
            }

            // create sectionlist for fastscrolling

            mSectionList = new ArrayList<String>();
            mSectionPositions = new ArrayList<Integer>();

            this.mCursor.moveToPosition(0);

            char lastSection = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)).toUpperCase().charAt(0);

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);

            for (int i = 1; i < this.mCursor.getCount(); i++) {

                this.mCursor.moveToPosition(i);

                char currentSection = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)).toUpperCase().charAt(0);

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);
                    mSectionPositions.add(i);

                    lastSection = currentSection;
                }

            }

            return super.swapCursor(c);
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            if (sectionIndex >= 0 && sectionIndex < mSectionPositions.size()) {
                return mSectionPositions.get(sectionIndex);
            }
            return 0;
        }

        @Override
        public int getSectionForPosition(int pos) {

            this.mCursor.moveToPosition(pos);

            String artistsName = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));

            char Artistsection = artistsName.toUpperCase().charAt(0);

            for (int i = 0; i < mSectionList.size(); i++) {

                if (Artistsection == mSectionList.get(i).toUpperCase().charAt(0)) {
                    return i;
                }

            }

            return 0;
        }

        @Override
        public Object[] getSections() {

            return mSectionList.toArray();
        }

    }

    // New loader needed
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new ArtistCoverLoader(getActivity());
        // return new CursorLoader(getActivity(),
        // MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
        // MusicLibraryHelper.projectionArtists, "", null,
        // MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Save scroll position
        mLastPosition = position;

        // identify current artist
        Cursor cursor = mCursorAdapter.getCursor();

        cursor.moveToPosition(position);

        String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
        long artistID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artist, artistID);

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
        Cursor cursorArtist = mCursorAdapter.getCursor();

        cursorArtist.moveToPosition(position);

        long artistID = cursorArtist.getLong(cursorArtist.getColumnIndex(MediaStore.Audio.Artists._ID));

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

}
