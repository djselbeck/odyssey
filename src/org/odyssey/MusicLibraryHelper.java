package org.odyssey;


import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class MusicLibraryHelper {
	private static final String TAG = "MusicLibraryHelper";
	public static final String[] projection = {MediaStore.Audio.AlbumColumns.ALBUM,
			MediaStore.Audio.Albums.ALBUM_KEY,
			MediaStore.Audio.Albums.NUMBER_OF_SONGS,
			MediaStore.Audio.Albums._ID,
			MediaStore.Audio.Albums.ALBUM_ART
	};
	
	public Cursor getAlbums(ContentResolver provider) {

		

		Cursor testCursor = provider.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
				projection, "", null, MediaStore.Audio.AlbumColumns.ALBUM);
		
		return testCursor;
	}
}
