package org.odyssey.fragments;

import java.util.ArrayList;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class AlbumsTracksFragment extends Fragment {

    private static final String TAG = "AlbumsTracksFragment";
    
    public final static String ARG_ALBUMKEY = "albumkey";
    public final static String ARG_ALBUMTITLE = "albumtitle";
    public final static String ARG_ALBUMART = "albumart";  
    public final static String ARG_ALBUMARTIST = "albumartist";

    private String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";
    
    private String orderBy = android.provider.MediaStore.Audio.Media.TRACK;  

    private String mAlbumKey = "";
    private String mAlbumTitle = "";
    private String mAlbumCoverPath = "";
    private String mAlbumArtist = "";
    private TrackListArrayAdapter mTrackListAdapter;
    
    private ImageView mCoverView;
    private TextView mAlbumTitleView;
    private TextView mAlbumArtistView;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                    Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_albumtracks, container,
		                false);
		
		// create listview header
		View headerView = inflater.inflate(R.layout.listview_header_item, null);
		
		mCoverView = (ImageView) headerView.findViewById(R.id.imageViewAlbumCover);
		
		mAlbumTitleView = (TextView) headerView.findViewById(R.id.textViewAlbumTitle);
		
		mAlbumArtistView = (TextView) headerView.findViewById(R.id.textViewArtistName);
		
		// create adapter for tracklist
		mTrackListAdapter = new TrackListArrayAdapter(getActivity(), R.layout.listview_tracklist_item, new ArrayList<TrackItem>());
		
		//create listview for tracklist
		ListView trackListView = (ListView) rootView.findViewById(R.id.listViewAlbumTrackList);
		
		trackListView.addHeaderView(headerView);
		
		trackListView.setAdapter(mTrackListAdapter);
		
		return rootView;
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

        String trackTitle = "";
        long trackDuration = 0;
        int trackNumber;
        String trackArtist ="";
        boolean isSampler = false;
        
        // get all tracks on the current album
        if (cursor.moveToFirst()) {
        	trackArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
	    	if(!trackArtist.equals(mAlbumArtist)){
				mAlbumArtistView.setText("Sampler");
				isSampler = true;
			}       	
            do {
            		trackTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            		trackDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            		trackNumber = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
            		
            		if(isSampler) {
            			trackArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            		} else {
            			trackArtist = "";
            		}
            		
                    mTrackListAdapter.add(new TrackItem(trackTitle,trackDuration,trackNumber,trackArtist));
            } while (cursor.moveToNext());         
        }
    }
    
    private class TrackListArrayAdapter extends ArrayAdapter<TrackItem> {
    	
    	private Context mContext;
    	private LayoutInflater mInflater;
    	private int mLayoutResourceId;
    	
    	public TrackListArrayAdapter(Context context, int layoutResourceId, ArrayList<TrackItem> data) {
    		super(context, layoutResourceId, data);
    		
    		mContext = context;
    		mLayoutResourceId = layoutResourceId;
    		mInflater = LayoutInflater.from(context);
    		
    	}
    	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
    		
			TextView trackTitleView;
			TextView trackDurationView;
			TextView trackNumberView;
			TextView trackArtistView;
			
			if(convertView == null) {
				convertView = mInflater.inflate(mLayoutResourceId, null);
			}
			
			trackTitleView = (TextView) convertView.findViewById(R.id.textViewTrackTitleItem);
			trackDurationView = (TextView) convertView.findViewById(R.id.textViewTrackDurationItem);
			trackNumberView = (TextView) convertView.findViewById(R.id.textViewTrackNumberItem);
			trackArtistView = (TextView) convertView.findViewById(R.id.textViewTrackArtistItem);
			
			// set tracktitle
			TrackItem trackItem = getItem(position);

			trackTitleView.setText(trackItem.trackTitle);	
			
			// calculate duration in minutes and seconds
			String seconds = String.valueOf((trackItem.trackDuration % 60000) / 1000);
			
			String minutes = String.valueOf(trackItem.trackDuration / 60000);
			
			if(seconds.length() == 1) {
				trackDurationView.setText(minutes + ":0" + seconds);
			} else {
				trackDurationView.setText(minutes + ":" + seconds);
			}
			
			// calculate track and discnumber
			if((""+trackItem.trackNumber).length() < 4) {
				trackNumberView.setText(""+trackItem.trackNumber);
			} else {
				
				//TODO shall we use discnumber?
				String discNumber = (""+trackItem.trackNumber).substring(0, 2);
				String trackNumber = (""+trackItem.trackNumber).substring(2);
				
				trackNumberView.setText(trackNumber);
			}
			
			// set artist if sampler
			trackArtistView.setText(trackItem.trackArtist);
			
			return convertView;
			
    	}
    	
    }
    
    // class for trackinformation
    private class TrackItem {
    	public String trackTitle;
    	public long trackDuration;
    	public int trackNumber;
    	public String trackArtist;
    	
    	public TrackItem(String title, long duration, int number, String artist) {
    		super();
    		this.trackDuration = duration;
    		this.trackTitle = title;
    		this.trackNumber = number;
    		this.trackArtist = artist;
    	}
    	
    }
}
