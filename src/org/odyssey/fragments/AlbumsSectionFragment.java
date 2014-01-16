package org.odyssey.fragments;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class AlbumsSectionFragment extends Fragment {

    AlbumCursorAdapter mCursorAdapter;	
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);
              
        MusicLibraryHelper libHelper = new MusicLibraryHelper();
        
        Cursor albumCursor = libHelper.getAlbums(getActivity().getContentResolver());
             
        mCursorAdapter = new AlbumCursorAdapter(getActivity(), albumCursor, 0);
        
        GridView mainGridView = (GridView) rootView;
        
        mainGridView.setNumColumns(2);
        
        mainGridView.setAdapter(mCursorAdapter);
        
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
			
			ImageView coverImage = (ImageView) view.findViewById(R.id.imageView1);
			
			String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
			
			if(imagePath != null)
			{
				Drawable tempImage = Drawable.createFromPath(imagePath);
				
				coverImage.setImageDrawable(tempImage);				
			}
			
			TextView albumLabel = (TextView) view.findViewById(R.id.textViewAlbumItem);
			
			albumLabel.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)));	
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
			
			View tempView = mInflater.inflate(R.layout.item_albums, null);
			
			ImageView coverImage = (ImageView) tempView.findViewById(R.id.imageView1);
			
			String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
			
			if(imagePath != null)
			{
				Drawable tempImage = Drawable.createFromPath(imagePath);
				
				coverImage.setImageDrawable(tempImage);				
			}
			
			TextView albumLabel = (TextView) tempView.findViewById(R.id.textViewAlbumItem);
			
			albumLabel.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)));
			
			return tempView;
		}
    	
    }
    
}
