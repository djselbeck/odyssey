package org.odyssey.fragments;

import java.util.ArrayList;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;
import org.odyssey.playbackservice.TrackItem;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class AllTracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    private static final String TAG = "AllTracksFragment";
    private ListView mListView = null;
    AllTracksCursorAdapter mCursorAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);

        View rootView = inflater.inflate(R.layout.fragment_alltracks, container, false);

        mCursorAdapter = new AllTracksCursorAdapter(getActivity(), null, 0);

        // create listview for tracklist
        mListView = (ListView) rootView.findViewById(R.id.listViewAllTracks);

        mListView.setAdapter(mCursorAdapter);

        mListView.setOnItemClickListener((OnItemClickListener) this);

        // register context menu
        registerForContextMenu(mListView);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Prepare loader ( start new one or reuse old)
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO
    }

    private class AllTracksCursorAdapter extends BaseAdapter implements SectionIndexer {

        private LayoutInflater mInflater;
        private Cursor mCursor;
        ArrayList<String> mSectionList;
        ArrayList<Integer> mSectionPositions;

        public AllTracksCursorAdapter(Context context, Cursor c, int flags) {

            super();

            mInflater = LayoutInflater.from(context);
            mCursor = c;
            mSectionList = new ArrayList<String>();
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

            String trackName = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));

            char trackSection = trackName.toUpperCase().charAt(0);

            for (int i = 0; i < mSectionList.size(); i++) {

                if (trackSection == mSectionList.get(i).toUpperCase().charAt(0)) {
                    Log.v(TAG, "Section for position: " + pos + " = " + i);
                    return i;
                }

            }

            return 0;
        }

        @Override
        public Object[] getSections() {

            return mSectionList.toArray();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TextView trackTitleView;
            TextView trackDurationView;
            TextView trackNumberView;
            TextView trackArtistView;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.listview_alltracks_item, null);
            }

            trackTitleView = (TextView) convertView.findViewById(R.id.textViewAllTracksTitleItem);
            trackDurationView = (TextView) convertView.findViewById(R.id.textViewAllTracksDurationItem);
            trackNumberView = (TextView) convertView.findViewById(R.id.textViewAllTracksNumberItem);
            trackArtistView = (TextView) convertView.findViewById(R.id.textViewAllTracksArtistItem);

            // set tracktitle
            TrackItem trackItem = (TrackItem) getItem(position);

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

            return convertView;
        }

        @Override
        public int getCount() {

            if (mCursor == null) {
                return 0;
            } else {
                return mCursor.getCount();
            }
        }

        @Override
        public Object getItem(int position) {

            // return trackitem

            mCursor.moveToPosition(position);

            String title = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            long duration = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            int no = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
            String artist = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            String url = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.DATA));

            TrackItem item = new TrackItem(title, artist, album, url, no, duration);

            return item;

        }

        @Override
        public long getItemId(int position) {
            // FIXME for now just return positon as id
            return position;
        }

        public Cursor swapCursor(Cursor c) {

            mCursor = c;

            if (mCursor == null) {
                return c;
            }

            // create sectionlist for fastscrolling

            mSectionList = new ArrayList<String>();
            mSectionPositions = new ArrayList<Integer>();

            this.mCursor.moveToPosition(0);

            int index = this.mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            char lastSection = 0;

            if (index > 0) {
                lastSection = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)).toUpperCase().charAt(0);
            }

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);

            for (int i = 1; i < this.mCursor.getCount(); i++) {

                this.mCursor.moveToPosition(i);

                char currentSection = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)).toUpperCase().charAt(0);

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);

                    lastSection = currentSection;
                    mSectionPositions.add(i);
                }

            }

            // notify for screen update
            notifyDataSetChanged();

            return c;

        }
    }

    private void playTrack(int position) {
        // clear playlist and play current track
        OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();

        try {
            app.getPlaybackService().clearPlaylist();
            enqueueTrack(position);
            app.getPlaybackService().jumpTo(0);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void enqueueTrack(int position) {
        // Enqueue single track
        OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();

        try {
            app.getPlaybackService().enqueueTrack((TrackItem) mCursorAdapter.getItem(position));
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void enqueueTrackAsNext(int position) {
        // Enqueue single track
        OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();

        try {
            app.getPlaybackService().enqueueTrackAsNext((TrackItem) mCursorAdapter.getItem(position));
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        playTrack(position);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.all_tracks_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.all_tracks_context_menu_action_enqueue:
            if (info.position >= 0) {
                enqueueTrack(info.position);
            }
            return true;
        case R.id.all_tracks_context_menu_action_enqueueasnext:
            if (info.position > 0) {
                enqueueTrackAsNext(info.position);
            }
            return true;
        case R.id.all_tracks_context_menu_action_play:
            playTrack(info.position);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(getActivity(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, "", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }
}
