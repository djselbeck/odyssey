package org.odyssey;

import org.odyssey.playbackservice.TrackItem;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;

public class MusicLibraryHelper {
    private static final String TAG = "MusicLibraryHelper";
    public static final String[] projectionAlbums = { MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_KEY, MediaStore.Audio.Albums.NUMBER_OF_SONGS, MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART,
            MediaStore.Audio.Albums.ARTIST };
    public static final String[] projectionArtists = { MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Artists.ARTIST_KEY, MediaStore.Audio.Artists.NUMBER_OF_TRACKS, MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS };
    public static final String[] projectionTracks = { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ALBUM_KEY, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA };

    /**
     * Resolves the url into an comfortably trackitem which contains artist and
     * title
     * 
     * @param url
     * @param resolver
     * @return
     */
    // FIXME ALBUM MISSING
    public static TrackItem getTrackItemFromURL(String url, ContentResolver resolver) {
        String selection = MediaStore.Audio.Media.DATA + "= ?";
        String[] selectionArgs = { url };
        Cursor trackCursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionTracks, selection, selectionArgs, MediaStore.Audio.Media.TITLE);

        String title = "";
        String artist = "";
        String album = "";
        int trackno = 0;
        long duration = 0;

        if (trackCursor != null && trackCursor.getCount() > 0) {
            trackCursor.moveToFirst();
            title = trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            artist = trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            album = trackCursor.getString(trackCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            trackno = trackCursor.getInt(trackCursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
            duration = trackCursor.getLong(trackCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        }
        return new TrackItem(title, artist, album, url, trackno, duration);
    }
}
