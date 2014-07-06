package org.odyssey.fragments;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class SavedPlaylistsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    PlaylistsCursorAdapter mCursorAdapter;
    ListView mListView;

    private static final String TAG = "OdysseyAllPlaylistsFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // set visibility of quickcontrols
        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);

        View rootView = inflater.inflate(R.layout.fragment_allplaylists, container, false);

        mCursorAdapter = new PlaylistsCursorAdapter(getActivity(), null, 0);

        mListView = (ListView) rootView.findViewById(R.id.listViewAllPlaylists);
        mListView.setAdapter(mCursorAdapter);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Prepare loader ( start new one or reuse old)
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        // TODO Auto-generated method stub

        return new CursorLoader(getActivity(), MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionPlaylists, "", null, MediaStore.Audio.Playlists.NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        mCursorAdapter.swapCursor(cursor);
        Log.v(TAG, "Loader finished");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
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
        public void bindView(View arg0, Context arg1, Cursor arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
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
}
