package org.odyssey.manager;

import org.odyssey.MusicLibraryHelper;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;

/*
 * Custom Loader for ARTIST with ALBUM_ART
 */
public class ArtistCoverLoader extends CursorLoader {

    private static final String TAG = "ArtistCoverLoader";

    Context mContext;

    public ArtistCoverLoader(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public Cursor loadInBackground() {
        Log.v(TAG, "load ArtistCovers");

        // get all album covers with corresponding artist
        Cursor albumArtCursor = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ARTIST }, "", null,
                MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE");

        // get all artists
        Cursor artistsCursor = mContext.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, "", null, MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE");

        // create a custom cursor to mix both
        MatrixCursor artistsCoverCursor = new MatrixCursor(new String[] { MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Artists.ARTIST_KEY, MediaStore.Audio.Artists.NUMBER_OF_TRACKS, MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS, MediaStore.Audio.Albums.ALBUM_ART });

        // join both cursor if match is found
        CursorJoiner artistCoverJoiner = new CursorJoiner(artistsCursor, new String[] { MediaStore.Audio.Artists.ARTIST }, albumArtCursor, new String[] { MediaStore.Audio.Albums.ARTIST });
        String artist, artistKey, cover;
        int numberOfTracks, numberOfAlbums;
        long artistID;

        for (CursorJoiner.Result joinerResult : artistCoverJoiner) {
            switch (joinerResult) {
            case LEFT:
                // no cover found
                artist = artistsCursor.getString(artistsCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                artistKey = artistsCursor.getString(artistsCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY));
                numberOfTracks = artistsCursor.getInt(artistsCursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
                artistID = artistsCursor.getLong(artistsCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
                numberOfAlbums = artistsCursor.getInt(artistsCursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
                cover = null;
                artistsCoverCursor.addRow(new Object[] { artist, artistKey, numberOfTracks, artistID, numberOfAlbums, cover });
                break;
            case RIGHT:
                // only cover found
                break;
            case BOTH:
                // artist and album cover match
                artist = artistsCursor.getString(artistsCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                artistKey = artistsCursor.getString(artistsCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY));
                numberOfTracks = artistsCursor.getInt(artistsCursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
                artistID = artistsCursor.getLong(artistsCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
                numberOfAlbums = artistsCursor.getInt(artistsCursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
                cover = albumArtCursor.getString(albumArtCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                artistsCoverCursor.addRow(new Object[] { artist, artistKey, numberOfTracks, artistID, numberOfAlbums, cover });
                break;
            }
        }

        // return new custom cursor
        return artistsCoverCursor;
    }

}
