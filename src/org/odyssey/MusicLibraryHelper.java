package org.odyssey;


import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class MusicLibraryHelper {
	private static final String TAG = "MusicLibraryHelper";
	public static final String[] projectionAlbums = {MediaStore.Audio.Albums.ALBUM,
			MediaStore.Audio.Albums.ALBUM_KEY,
			MediaStore.Audio.Albums.NUMBER_OF_SONGS,
			MediaStore.Audio.Albums._ID,
			MediaStore.Audio.Albums.ALBUM_ART,
			MediaStore.Audio.Albums.ARTIST
	};
	public static final String[] projectionArtists = {MediaStore.Audio.Artists.ARTIST,
		MediaStore.Audio.Artists.ARTIST_KEY,
		MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
		MediaStore.Audio.Artists._ID,
		MediaStore.Audio.Artists.NUMBER_OF_ALBUMS		
};	
	public static final String[] projectionTracks = {MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DISPLAY_NAME, 
        MediaStore.Audio.Media.TRACK, 
        MediaStore.Audio.Media.ALBUM_KEY, 
        MediaStore.Audio.Media.ALBUM		
};	
	
//	public Cursor getAlbums(ContentResolver provider) {
//
//		Cursor testCursor = provider.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//				projectionAlbums, "", null, MediaStore.Audio.AlbumColumns.ALBUM);
//		
//		return testCursor;
//	}
}
