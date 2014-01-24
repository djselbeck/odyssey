package org.odyssey.fragments;

import java.util.ArrayList;

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

import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ListView;


public class AlbumsTracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

	private static final String TAG = "AlbumsTracksFragment";
	
	public final static String ARG_ALBUMKEY = "albumkey";	

    private String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";  
    
    private String orderBy = android.provider.MediaStore.Audio.Media.DISPLAY_NAME;	

    private String mAlbumKey = "";
    private TracksCursorAdapter mTrackCursorAdapter;
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_albumtracks, container,
				false);
		
		mTrackCursorAdapter = new TracksCursorAdapter(getActivity(), null, 0);
		
		ImageView coverView = (ImageView) rootView.findViewById(R.id.imageViewAlbum2);
		
		coverView.setImageResource(R.drawable.coverplaceholder);
		
		ListView listView = (ListView) rootView.findViewById(R.id.listViewTracks);	
		
		listView.setAdapter(mTrackCursorAdapter);

		return rootView;
	}    
    
    @Override
    public void onStart() {
        super.onStart();
        
		// Prepare loader ( start new one or reuse old)
		getLoaderManager().initLoader(0, getArguments(), this);          
        
    }    
    
    private class TracksCursorAdapter extends CursorAdapter {

		private LayoutInflater mInflater;
		private Cursor mCursor;   	
    	
		public TracksCursorAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
			
			this.mInflater = LayoutInflater.from(context);
			this.mCursor = c;
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

			int labelIndex = 0;

			if (convertView == null) {

				convertView = mInflater.inflate(R.layout.item_tracks, null);

			} 
			
			TextView labelView = (TextView) convertView.findViewById(R.id.textViewTracksItem);
			
			// get labeltext
			if (this.mCursor == null) {
				return convertView;
			}

			this.mCursor.moveToPosition(position);

			labelIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);

			if (labelIndex >= 0) {
				String labelText = mCursor.getString(labelIndex);
				if (labelText != null) {
					labelView.setText(labelText);
				}
			} else {
				// placeholder for empty labels
				labelView.setText("");
			}
			
			return convertView;
		}
		
		@Override
		public Cursor swapCursor(Cursor c) {

			this.mCursor = c;

			return super.swapCursor(c);
		}		
    	
    }

	// New loader needed
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

		mAlbumKey = bundle.getString(ARG_ALBUMKEY);
		
		String[] whereVal = {mAlbumKey};			
		
		return new CursorLoader(getActivity(),
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MusicLibraryHelper.projectionTracks, where, whereVal, orderBy);					
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mTrackCursorAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mTrackCursorAdapter.swapCursor(null);
	}
}
