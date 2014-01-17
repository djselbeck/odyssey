package org.odyssey.fragments;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class AlbumsSectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    AlbumCursorAdapter mCursorAdapter;	
    
    private static final String TAG = "AlbumsSectionFragment"; 
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);
              
        //MusicLibraryHelper libHelper = new MusicLibraryHelper();
                     
        mCursorAdapter = new AlbumCursorAdapter(getActivity(), null, 0);
        
        GridView mainGridView = (GridView) rootView;
        
        mainGridView.setNumColumns(2);
        
        mainGridView.setAdapter(mCursorAdapter);
        
        // Prepare loader ( start new one or reuse old) 
        getLoaderManager().initLoader(0, null, this);
        
        return rootView;
    }	
	
    private class AlbumCursorAdapter extends CursorAdapter {

    	private LayoutInflater mInflater;
    	
		public AlbumCursorAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
			
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			int index;
			
			ImageView coverImage = (ImageView) view.findViewById(R.id.imageView1);
			
			index = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
			
			if ( index >= 0 ) {
				String imagePath = cursor.getString(index);
				if(imagePath != null)
				{
					Drawable tempImage = Drawable.createFromPath(imagePath);
					coverImage.setImageDrawable(tempImage);				
				} else {
					coverImage.setImageResource(R.drawable.coverplaceholder);
				}
			}
			
			TextView albumLabel = (TextView) view.findViewById(R.id.textViewAlbumItem);
			
			index = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
			if ( index >= 0 ) {
				albumLabel.setText(cursor.getString(index));
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
			
			View tempView = mInflater.inflate(R.layout.item_albums, null);
			int index = 0;
			
			ImageView coverImage = (ImageView) tempView.findViewById(R.id.imageView1);
			
			try {
				index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART);
			}
			catch (IllegalArgumentException exception )
			{
				Log.e(TAG,"Column ALBUM_ART not found");
				exception.printStackTrace();
			}
			
			if ( index >= 0 ) {
				String imagePath = cursor.getString(index);
				if(imagePath != null)
				{
					Drawable tempImage = Drawable.createFromPath(imagePath);
					Log.v(TAG,"minimum: " + tempImage.getMinimumHeight()+ ":" + tempImage.getMinimumWidth());
					Log.v(TAG,"intrinsic: " + tempImage.getIntrinsicHeight()+ ":" + tempImage.getIntrinsicWidth());
					coverImage.setImageDrawable(tempImage);				
				} else {
					coverImage.setImageResource(R.drawable.coverplaceholder);
				}
			}
			
			TextView albumLabel = (TextView) tempView.findViewById(R.id.textViewAlbumItem);
			
			index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
			if ( index >= 0 ) {
				albumLabel.setText(cursor.getString(index));
			}
			
			return tempView;
		}
    	
    }

    
    // New loader needed
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		return new CursorLoader(getActivity(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,  MusicLibraryHelper.projection, "", null, MediaStore.Audio.AlbumColumns.ALBUM);
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
