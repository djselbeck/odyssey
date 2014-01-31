package org.odyssey;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;

public class MusicLibraryHelper {
	private static final String TAG = "MusicLibraryHelper";
	public static final String[] projectionAlbums = {
			MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_KEY,
			MediaStore.Audio.Albums.NUMBER_OF_SONGS,
			MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART,
			MediaStore.Audio.Albums.ARTIST };
	public static final String[] projectionArtists = {
			MediaStore.Audio.Artists.ARTIST,
			MediaStore.Audio.Artists.ARTIST_KEY,
			MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
			MediaStore.Audio.Artists._ID,
			MediaStore.Audio.Artists.NUMBER_OF_ALBUMS };
	public static final String[] projectionTracks = {
			MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME,
			MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ALBUM_KEY,
			MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM,
			MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA };

	// class for trackinformation
	public static class TrackItem {
		public String trackTitle;
		public long trackDuration;
		public int trackNumber;
		public String trackArtist;
		public String trackURL;

		public TrackItem() {
			super();
			this.trackTitle = "";
			this.trackArtist = "";
			this.trackDuration = 0;
			this.trackNumber = 0;
			this.trackURL = "";
		}

		public TrackItem(String title, long duration, int number,
				String artist, String url) {
			super();
			this.trackDuration = duration;
			this.trackTitle = title;
			this.trackNumber = number;
			this.trackArtist = artist;
			this.trackURL = url;
		}
		
		public String toString() {
			return "Title: " + trackTitle + " Artist: " + trackArtist + " URL: " + trackURL + " No.: " + trackNumber + " Duration(s): " + trackDuration;
		}
	}
	
	/**
	 * Resolves the url into an comfortably trackitem which contains artist and title
	 * @param url
	 * @param resolver
	 * @return
	 */
	//FIXME ALBUM MISSING
	public static TrackItem getTrackItemFromURL(String url, ContentResolver resolver) {
		TrackItem tmpItem = new TrackItem();
		String selection = MediaStore.Audio.Media.DATA + "= ?";
		String[] selectionArgs = {url};
		Cursor trackCursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionTracks, selection, selectionArgs, MediaStore.Audio.Media.TITLE);
		
		if ( trackCursor != null && trackCursor.getCount() > 0) {
			trackCursor.moveToFirst();
			String title = trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
			String artist = trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
			int number = trackCursor.getInt(trackCursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
			long duration = trackCursor.getLong(trackCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
			tmpItem.trackTitle = title;
			tmpItem.trackArtist = artist;
			tmpItem.trackNumber = number;
			tmpItem.trackDuration = duration;
		}
		tmpItem.trackURL = url;
		return tmpItem;
	}
}
