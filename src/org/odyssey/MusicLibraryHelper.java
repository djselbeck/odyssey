package org.odyssey;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class MusicLibraryHelper {
	private static final String TAG = "MusicLibraryHelper";
	
	public ArrayList<String> getAlbums(ContentResolver provider) {
		ArrayList<String> albums = new ArrayList<String>();
		String[] projection = {MediaStore.Audio.AlbumColumns.ALBUM,
				MediaStore.Audio.AlbumColumns.ALBUM_KEY,
				MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS
		};
		String[] selectionArgs = {""};
		Cursor testCursor = provider.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
				projection, "", null, MediaStore.Audio.AlbumColumns.ALBUM);
		
		Log.v(TAG, "Helper got: "+ testCursor.getCount() + " albums");
		while (testCursor.moveToNext() ) {
			String album = testCursor.getString(0);
			Log.v(TAG, "Adding album: " + album);
			albums.add(album);
			
		}
		
		Log.v(TAG, "Helper got: "+ testCursor.getCount() + " albums");
		return albums;
	}
}
