package org.odyssey.fragments;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.R;
import org.odyssey.manager.AsyncLoader;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class ArtistsSectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    ArtistsCursorAdapter mCursorAdapter;
    ArrayList<String> mSectionList;
    
    private static final String TAG = "ArtistsSectionFragment"; 
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);
                     
        mCursorAdapter = new ArtistsCursorAdapter(getActivity(), null, 0);
        
        GridView mainGridView = (GridView) rootView;
        
        mainGridView.setNumColumns(2);
        
        mainGridView.setAdapter(mCursorAdapter);
        
        // Prepare loader ( start new one or reuse old) 
        getLoaderManager().initLoader(0, null, this);
        
        return rootView;
    }	
	
    private class ArtistsCursorAdapter extends CursorAdapter implements SectionIndexer {

    	private LayoutInflater mInflater;
    	private Cursor mCursor;
    	private LruCache<String, Drawable> mCoverCache;
    	
		public ArtistsCursorAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
			
			this.mInflater = LayoutInflater.from(context);
			this.mCursor = c;
			
			// create cache for drawable objects with given size
			mCoverCache = new LruCache<String, Drawable>(18);
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
		public View getView(int position, View convertView, ViewGroup parent){
				
			Log.v(TAG, "Index: "+position);
			
			int index = 0;
			
			ImageView coverImage;
			TextView artistsLabel;
			
			if(convertView == null){
				
				convertView = mInflater.inflate(R.layout.item_artists, null);
						
			} 
			
			if(this.mCursor == null){
				return convertView;
			}
			
			this.mCursor.moveToPosition(position);			
			
			coverImage = (ImageView) convertView.findViewById(R.id.imageViewArtists);	
			
			artistsLabel = (TextView) convertView.findViewById(R.id.textViewArtistsItem);
			
//			index = mCursor.getColumnIndex(MediaStore.Audio.Artists);	
			
			// set default cover
			coverImage.setImageResource(R.drawable.coverplaceholder);
			
//			if ( index >= 0 ) {
//				String imagePath = mCursor.getString(index);
//				if(imagePath != null)
//				{		
//					// check cache
//					Drawable tempCover = mCoverCache.get(imagePath);
//					
//					if(tempCover == null){
//						
//						// cache miss start async loading
//						AsyncLoader coverLoader = new AsyncLoader();
//						
//						AsyncLoader.CoverViewHolder coverHolder = new AsyncLoader.CoverViewHolder();
//						
//						coverHolder.imagePath = imagePath;
//						coverHolder.coverView = coverImage;
//						
//						// save drawable in cache
//						coverHolder.coverCache = new WeakReference<LruCache<String,Drawable>>(mCoverCache);
//						
//						coverLoader.execute(coverHolder);						
//						
//					} else{
//						coverImage.setImageDrawable(tempCover);
//					}
//			
//				}
//			}
				
			index = mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
			if ( index >= 0 ) {
				artistsLabel.setText(mCursor.getString(index));
			}	
			
			return convertView;
		
		}
		
		
		@Override
		public Cursor swapCursor(Cursor c){
			
			this.mCursor = c;
			
			if(mCursor == null){
				return super.swapCursor(c);
			}			
			
			// create sectionlist for fastscrolling
			
			mSectionList = new ArrayList<String>(); 
					
			this.mCursor.moveToPosition(0);
			
			char lastSection = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)).charAt(0);
			
			mSectionList.add(""+lastSection);
			
			for(int i = 1; i < this.mCursor.getCount(); i++){
			
				this.mCursor.moveToPosition(i);
				
				char currentSection = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)).charAt(0);
				
				if(lastSection != currentSection){
					mSectionList.add(""+currentSection);
					
					lastSection = currentSection;
				}
				
			}			
			
			return super.swapCursor(c);
		}

		@Override
		public int getPositionForSection(int sectionIndex) {
			
			char section = mSectionList.get(sectionIndex).charAt(0);
			
			for(int i = 0; i < this.mCursor.getCount(); i++){
				
				this.mCursor.moveToPosition(i);
				
				char currentSection = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)).charAt(0);
				
				if(section == currentSection){
					return i;
				}
				
			}				
			
			return 0;
		}

		@Override
		public int getSectionForPosition(int pos) {
			
			this.mCursor.moveToPosition(pos);
			
			String artistsName = this.mCursor.getString(this.mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
			
			char Artistsection = artistsName.charAt(0);
			
			for( int i = 0; i < mSectionList.size(); i++){
				
				if(Artistsection == mSectionList.get(i).charAt(0)){
					return i;
				}
				
			}
			
			return 0;
		}

		@Override
		public Object[] getSections() {
			
			return mSectionList.toArray();
		}
    	
    }

    
    // New loader needed
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		return new CursorLoader(getActivity(), MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,  MusicLibraryHelper.projectionArtists, "", null, MediaStore.Audio.Artists.ARTIST);
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
