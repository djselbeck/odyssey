package org.odyssey.fragments;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class AlbumsTracksFragment extends ListFragment {

    private static final String TAG = "AlbumsTracksFragment";
    
    public final static String ARG_ALBUMKEY = "albumkey";
    public final static String ARG_ALBUMTITLE = "albumtitle";
    public final static String ARG_ALBUMART = "albumart";  
    public final static String ARG_ALBUMARTIST = "albumartist";

    private String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";
    
    private String orderBy = android.provider.MediaStore.Audio.Media.DISPLAY_NAME;        

    private String mAlbumKey = "";
    private String mAlbumTitle = "";
    private String mAlbumCoverPath = "";
    private String mAlbumArtist = "";
    private ArrayAdapter<String> mTrackListAdapter;
    
    private ImageView mCoverView;
    private TextView mAlbumTitleView;
    private TextView mAlbumArtistView;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                    Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_albumtracks, container,
		                false);
		
		mCoverView = (ImageView) rootView.findViewById(R.id.imageViewAlbumCover);
		
		mAlbumTitleView = (TextView) rootView.findViewById(R.id.textViewAlbumTitle);
		
		mAlbumArtistView = (TextView) rootView.findViewById(R.id.textViewArtistName);
		
		return rootView;
    }     
    
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
        mAlbumTitle = args.getString(ARG_ALBUMTITLE);
        mAlbumCoverPath = args.getString(ARG_ALBUMART);
        mAlbumArtist = args.getString(ARG_ALBUMARTIST);
        
        setAlbumInformation();
        
        setAlbumTracks();
    }
    
    private void setAlbumInformation() {
    	
    	if(mAlbumCoverPath != null) {
    		mCoverView.setImageDrawable(Drawable.createFromPath(mAlbumCoverPath));
    	} else {
    		mCoverView.setImageResource(R.drawable.coverplaceholder);
    	}
    	
    	mAlbumTitleView.setText(mAlbumTitle);
    	
    	mAlbumArtistView.setText(mAlbumArtist);
    }
    
    private void setAlbumTracks() {         

        String whereVal[] = {mAlbumKey};

        Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MusicLibraryHelper.projectionTracks, where, whereVal, orderBy);

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
