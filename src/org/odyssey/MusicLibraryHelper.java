package org.odyssey;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class MusicLibraryHelper {
	private static final String TAG = "MusicLibraryHelper";
	
	public Cursor getAlbums(ContentResolver provider) {
		ArrayList<String> albums = new ArrayList<String>();
		String[] projection = {MediaStore.Audio.AlbumColumns.ALBUM,
				MediaStore.Audio.Albums.ALBUM_KEY,
				MediaStore.Audio.Albums.NUMBER_OF_SONGS,
				MediaStore.Audio.Albums._ID,
				MediaStore.Audio.Albums.ALBUM_ART
		};
		String[] selectionArgs = {""};
		Cursor testCursor = provider.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
				projection, "", null, MediaStore.Audio.AlbumColumns.ALBUM);
		
		return testCursor;
	}
}
