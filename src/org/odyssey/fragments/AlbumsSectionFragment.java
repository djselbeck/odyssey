package org.odyssey.fragments;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;
import org.odyssey.fragments.ArtistsSectionFragment.OnArtistSelectedListener;
import org.odyssey.manager.AsyncLoader;
import org.odyssey.manager.AsyncLoader.CoverViewHolder;
import org.odyssey.playbackservice.TrackItem;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

public class AlbumsSectionFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

	AlbumCursorAdapter mCursorAdapter;
	ArrayList<String> mSectionList;
	//FIXME listener in new file?
	OnAlbumSelectedListener mAlbumSelectedCallback;
	OnArtistSelectedListener mArtistSelectedCallback;
	  	
	private String mArtist = "";    
	private long mArtistID = -1; 

	public final static String ARG_ARTISTNAME = "artistname";	
	public final static String ARG_ARTISTID = "artistid";
	
	private static final String TAG = "AlbumsSectionFragment";
	
	private GridView mRootGrid;
	private int mLastPosition;
	
	// Listener for communication via container activity
	public interface OnAlbumSelectedListener {
		public void onAlbumSelected(String albumKey, String albumTitle, String albumCoverImagePath, String albumArtist);
	}
	
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
        	mAlbumSelectedCallback = (OnAlbumSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnAlbumSelectedListener");
        }
        
        try {
        	mArtistSelectedCallback = (OnArtistSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnArtistSelectedListener");
        }        
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);	
		
		View rootView = inflater.inflate(R.layout.fragment_albums, container,
				false);

		mCursorAdapter = new AlbumCursorAdapter(getActivity(), null, 0);

		mRootGrid = (GridView) rootView;

		mRootGrid.setNumColumns(2);

		mRootGrid.setAdapter(mCursorAdapter);
		
		mRootGrid.setOnItemClickListener((OnItemClickListener) this);	
		
		if(getArguments() != null) {
	        // update actionbar
	        final ActionBar actionBar = getActivity().getActionBar();
	
	        actionBar.setHomeButtonEnabled(true);
	        // allow backnavigation by homebutton 
	        actionBar.setDisplayHomeAsUpEnabled(true);
	        
			mRootGrid.setFastScrollEnabled(false);
			mRootGrid.setFastScrollAlwaysVisible(false);	        
		}		
		
		// register context menu
		registerForContextMenu(mRootGrid);

		return rootView;
	}
	
    @Override
    public void onStart() {
        super.onStart();
    	
		// Prepare loader ( start new one or reuse old)
		getLoaderManager().initLoader(0, getArguments(), this);    	
    }	
    
    @Override
    public void onResume() {
    	super.onResume();
    	if ( mLastPosition >= 0 ) {
    		mRootGrid.setSelection(mLastPosition);
    		mLastPosition = -1;
    	}
    }

	private class AlbumCursorAdapter extends CursorAdapter implements
			SectionIndexer {

		private LayoutInflater mInflater;
		private Cursor mCursor;
		private LruCache<String, Drawable> mCache;

		public AlbumCursorAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);

			this.mInflater = LayoutInflater.from(context);
			this.mCursor = c;
			this.mCache = new LruCache<String, Drawable>(24);
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

			Log.v(TAG, "Index: " + position);

			int coverIndex = 0;
			int labelIndex = 0;

			AsyncLoader.CoverViewHolder coverHolder = null;

			if (convertView == null) {

				convertView = mInflater.inflate(R.layout.item_albums, null);
				
				// create new coverholder for imageview(cover) and
				// textview(albumlabel)
				coverHolder = new AsyncLoader.CoverViewHolder();
				coverHolder.coverView = (ImageView) convertView
						.findViewById(R.id.imageViewAlbum);
				coverHolder.labelView = (TextView) convertView
						.findViewById(R.id.textViewAlbumItem);

				convertView.setTag(coverHolder);

			} else {
				// get coverholder from convertview and cancel asynctask
				coverHolder = (CoverViewHolder) convertView.getTag();
				if (coverHolder.task != null)
					coverHolder.task.cancel(true);
			}

			// set default cover

			// get imagepath and labeltext
			if (this.mCursor == null) {
				return convertView;
			}

			// Set position of item for onclickListener
//			((AlbumItem)convertView).setPosition(position);
			
			this.mCursor.moveToPosition(position);

			coverIndex = mCursor
					.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
			labelIndex = mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);

			if (labelIndex >= 0) {
				// coverHolder.labelText = mCursor.getString(labelIndex);
				String labelText = mCursor.getString(labelIndex);
				if (labelText != null) {
					coverHolder.labelView.setText(labelText);
				}
			} else {
				// placeholder for empty labels
				coverHolder.labelView.setText("");
			}

			// Check for valid column
			if (coverIndex >= 0) {
				// Get column value (Image-URL)
				coverHolder.imagePath = mCursor.getString(coverIndex);
				if (coverHolder.imagePath != null) {
					// Check cache first
					Drawable cacheImage = mCache.get(coverHolder.imagePath);
					if (cacheImage == null) {
						// Cache miss
						// create and execute new asynctask
						coverHolder.task = new AsyncLoader();
						coverHolder.cache = new WeakReference<LruCache<String, Drawable>>(
								mCache);
						coverHolder.task.execute(coverHolder);
					} else {
						// Cache hit
						coverHolder.coverView.setImageDrawable(cacheImage);
					}
				} else {
					// Cover entry has no album art
					coverHolder.coverView
							.setImageResource(R.drawable.coverplaceholder);
				}
			} else {
				coverHolder.coverView
						.setImageResource(R.drawable.coverplaceholder);
				coverHolder.imagePath = null;
			}
			
			return convertView;
		}

		@Override
		public Cursor swapCursor(Cursor c) {

			this.mCursor = c;

			if (mCursor == null) {
				return super.swapCursor(c);
			}

			// create sectionlist for fastscrolling

			mSectionList = new ArrayList<String>();

			this.mCursor.moveToPosition(0);

			int index = this.mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
			char lastSection = 0;

			if( index > 0) {
				lastSection = this.mCursor.getString(
						this.mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM))
						.charAt(0);
			}


			mSectionList.add("" + lastSection);

			for (int i = 1; i < this.mCursor.getCount(); i++) {

				this.mCursor.moveToPosition(i);

				char currentSection = this.mCursor.getString(
						this.mCursor
								.getColumnIndex(MediaStore.Audio.Albums.ALBUM))
						.charAt(0);

				if (lastSection != currentSection) {
					mSectionList.add("" + currentSection);

					lastSection = currentSection;
				}

			}

			return super.swapCursor(c);
		}

		@Override
		public int getPositionForSection(int sectionIndex) {

			char section = mSectionList.get(sectionIndex).charAt(0);

			for (int i = 0; i < this.mCursor.getCount(); i++) {

				this.mCursor.moveToPosition(i);

				char currentSection = this.mCursor.getString(
						this.mCursor
								.getColumnIndex(MediaStore.Audio.Albums.ALBUM))
						.charAt(0);

				if (section == currentSection) {
					return i;
				}

			}

			return 0;
		}

		@Override
		public int getSectionForPosition(int pos) {

			this.mCursor.moveToPosition(pos);

			String albumName = this.mCursor.getString(this.mCursor
					.getColumnIndex(MediaStore.Audio.Albums.ALBUM));

			char albumSection = albumName.charAt(0);

			for (int i = 0; i < mSectionList.size(); i++) {

				if (albumSection == mSectionList.get(i).charAt(0)) {
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
		
		if(bundle == null) {
			
			// all albums
			
			return new CursorLoader(getActivity(),
					MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
					MusicLibraryHelper.projectionAlbums, "", null,
					MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");			
			
		} else {
			
			// only albums of artist mArtist			
	
			mArtist = bundle.getString(ARG_ARTISTNAME);
			mArtistID = bundle.getLong(ARG_ARTISTID);
			Log.v(TAG,"Getting albums for: " + mArtist + " with ID: " + mArtistID );
			
			String[] whereVal = {mArtist};
			
			return new CursorLoader(getActivity(),
					MediaStore.Audio.Artists.Albums.getContentUri("external", mArtistID),
					MusicLibraryHelper.projectionAlbums, "", null,
					MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE");					
		}		
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mCursorAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mCursorAdapter.swapCursor(null);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// Save position for later resuming
		mLastPosition = position;
		
		//identify current album
		Cursor cursor = mCursorAdapter.getCursor();
		
		cursor.moveToPosition(position);
		
		String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY));
		String albumTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
		String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
		String artistTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
		
		// Send the event to the host activity
		mAlbumSelectedCallback.onAlbumSelected(albumKey, albumTitle, imagePath, artistTitle);
		
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    MenuInflater inflater = getActivity().getMenuInflater();
	    inflater.inflate(R.menu.album_context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {	
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		if(info == null) {
			return super.onContextItemSelected(item);
		}
		
	    switch (item.getItemId()) {
	        case R.id.album_context_menu_action_enqueue:
	            enqueueAlbum(info.position);
	            return true;
	        case R.id.album_context_menu_action_play:
	        	playAlbum(info.position);
	            return true;
	        case R.id.album_context_menu_action_artist:
	        	showArtist(info.position);
	            return true;    
	        default:
	            return super.onContextItemSelected(item);
	    }
	}	
	
	private void enqueueAlbum(int position) {
		//identify current album
		Cursor albumCursor = mCursorAdapter.getCursor();
		
		albumCursor.moveToPosition(position);
		
		String albumKey = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY));		
		
		OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();	
		
		// get and enqueue albumtracks
		
		String whereVal[] = { albumKey };

		String where = android.provider.MediaStore.Audio.Media.ALBUM_KEY + "=?";

		String orderBy = android.provider.MediaStore.Audio.Media.TRACK;		
		
		Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
													MusicLibraryHelper.projectionTracks, where, whereVal, orderBy);

		// get all tracks on the current album
		if (cursor.moveToFirst()) {
			do {
				String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
				long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
				int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
				String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
				String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

				TrackItem item = new TrackItem(title,artist,"",url,no,duration);
				
				// enqueue current track
				try {
					app.getPlaybackService().enqueueTrack(item);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} while (cursor.moveToNext());
		}		
	}
	
	private void playAlbum(int position) {	

		OdysseyApplication app = (OdysseyApplication) getActivity().getApplication();
		
		// Remove old tracks
		try {
			app.getPlaybackService().clearPlaylist();
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
		
		// get and enqueue albumtracks
		enqueueAlbum(position);		
		
		// play album
		try {
			app.getPlaybackService().jumpTo(0);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}							
	}
	
	private void showArtist(int position) {
		//identify current artist
		Cursor cursor = mCursorAdapter.getCursor();
		
		cursor.moveToPosition(position);

		String artistTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));	
		
		//get artist id
		String whereVal[] = { artistTitle };

		String where = android.provider.MediaStore.Audio.Artists.ARTIST + "=?";

		String orderBy = android.provider.MediaStore.Audio.Artists.ARTIST+ " COLLATE NOCASE";			
		
		Cursor artistCursor = getActivity().getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, 
				MusicLibraryHelper.projectionArtists, where, whereVal, orderBy);
		
		artistCursor.moveToFirst();
		
		long artistID = artistCursor.getLong(artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
		
		// Send the event to the host activity
		mArtistSelectedCallback.onArtistSelected(artistTitle,artistID);		
	}
}
