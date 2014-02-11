package org.odyssey.fragments;

import java.util.ArrayList;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
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

        // mListView.setAdapter(mCursorAdapter);

        mListView.setOnItemClickListener((OnItemClickListener) this);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Prepare loader ( start new one or reuse old)
        // getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO
    }

    private class AllTracksCursorAdapter extends BaseAdapter implements SectionIndexer {

        private LayoutInflater mInflater;
        private Cursor mCursor;

        public AllTracksCursorAdapter(Context context, Cursor c, int flags) {

            super();

            this.mInflater = LayoutInflater.from(context);
            this.mCursor = c;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getSectionForPosition(int pos) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Object[] getSections() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {

                convertView = mInflater.inflate(R.layout.listview_alltracks_item, null);
            }

            return convertView;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return null;// new CursorLoader(getActivity(),
                    // MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    // MusicLibraryHelper.projectionTracks, "", null,
                    // MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // mCursorAdapter.swapCursor(null);
    }
}
