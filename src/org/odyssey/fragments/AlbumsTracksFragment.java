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
	
	public final static String ARG_ALBUMKEY = "albumkey";	
	
    private String[] column = { MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME, 
            MediaStore.Audio.Media.TRACK, 
            MediaStore.Audio.Media.ALBUM_KEY, 
            MediaStore.Audio.Media.ALBUM };

    private String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";  
    
    private String orderBy = android.provider.MediaStore.Audio.Media.DISPLAY_NAME;	

    private String mAlbumKey = "";
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
        
        mAlbumKey = args.getString(ARG_ALBUMKEY);       
        
        setAlbumTracks(mAlbumKey);
    }    
    
    private void setAlbumTracks(String albumKey) {

    	// set cursor to position
    	Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
				MusicLibraryHelper.projectionAlbums, "", null,
				MediaStore.Audio.Albums.ALBUM);	    	

        String whereVal[] = {albumKey};

        cursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                column, where, whereVal, orderBy);

        String trackID = "";
        
        // get all tracks on the current album
        if (cursor.moveToFirst()) {
        	mTrackListAdapter.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
            do {
            	trackID = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            	mTrackListAdapter.add(trackID);
            } while (cursor.moveToNext());        	
        }    
    }   
}
