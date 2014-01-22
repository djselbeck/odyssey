package org.odyssey.fragments;

import org.odyssey.MusicLibraryHelper;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;

import android.widget.ArrayAdapter;


public class AlbumsTracksFragment extends ListFragment {

	private static final String TAG = "AlbumsTracksFragment";
	
	public final static String ARG_POSITION = "position";	
	
    private String[] column = { MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.TRACK };

    private String where = android.provider.MediaStore.Audio.Media.ALBUM + "=?";  
    
    private String orderBy = android.provider.MediaStore.Audio.Media.DISPLAY_NAME;	

    private int mAlbumId = 0;
    private ArrayAdapter<String> mTrackListAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
		mTrackListAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1);
		
		setListAdapter(mTrackListAdapter);    	
		
		super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        
    	mAlbumId = args.getInt(ARG_POSITION);       
        
        setAlbumTracks(mAlbumId);
    }    
    
    private void setAlbumTracks(int position) {

    	// set cursor to position
    	Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
				MusicLibraryHelper.projectionAlbums, "", null,
				MediaStore.Audio.Albums.ALBUM);
    	
    	cursor.moveToPosition(position);
    	
    	// get the index of the current album
		int index = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
		if ( index >= 0 ) {
			mTrackListAdapter.add("Album: " + cursor.getString(index));
		}	    	

        String whereVal[] = {cursor.getString(index)};

        cursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                column, where, whereVal, orderBy);

        String trackID = "";
        
        // get all tracks on the current album
        if (cursor.moveToFirst()) {
            do {
            	trackID = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            	mTrackListAdapter.add(trackID);
            } while (cursor.moveToNext());        	
        }    
    }   
}
