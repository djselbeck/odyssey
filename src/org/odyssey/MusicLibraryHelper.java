package org.odyssey;

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
			this.trackURL = null;
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

	}
}
