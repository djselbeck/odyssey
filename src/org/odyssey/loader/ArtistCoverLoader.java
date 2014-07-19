package org.odyssey.loader;

import java.util.ArrayList;
import java.util.List;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.databasemodel.ArtistModel;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

/*
 * Custom Loader for ARTIST with ALBUM_ART
 */
public class ArtistCoverLoader extends AsyncTaskLoader<List<ArtistModel>> {

    private static final String TAG = "OdysseyArtistLoader";

    Context mContext;

    public ArtistCoverLoader(Context context) {
        super(context);
        this.mContext = context;

        forceLoad();
    }

    @Override
    public List<ArtistModel> loadInBackground() {
        Log.v(TAG, "load ArtistCovers");

        // get all album covers
        Cursor cursorAlbumArt = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM }, "", null,
                MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE");

        // get all artists
        Cursor cursorArtists = mContext.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, "", null, MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE");

        // create a custom cursor to mix both
        // MatrixCursor cursorArtistCover = new MatrixCursor(new String[] {
        // MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Artists.ARTIST_KEY,
        // MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
        // MediaStore.Audio.Artists._ID,
        // MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
        // MediaStore.Audio.Albums.ALBUM_ART });

        ArrayList<ArtistModel> artists = new ArrayList<ArtistModel>();

        // join both cursor if match is found
        String artist, artistKey, cover;
        int numberOfTracks, numberOfAlbums;
        long artistID;
        boolean foundCover = false;
        int pos = 0;

        if (cursorArtists.moveToFirst()) {
            do {
                artist = cursorArtists.getString(cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                Log.v(TAG, "Checking artist: " + artist);

                if (cursorAlbumArt.moveToPosition(pos)) {
                    foundCover = false;
                    do {
                        String albumArtist = cursorAlbumArt.getString(cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                        cover = cursorAlbumArt.getString(cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                        if (artist.equals(albumArtist) && cover != null && !cover.equals("")) {
                            Log.v(TAG, "Found art album artist: " + albumArtist + " and album: " + cursorAlbumArt.getString(cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ALBUM)));
                            foundCover = true;
                            // artist and album cover match
                            artist = cursorArtists.getString(cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                            artistKey = cursorArtists.getString(cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY));
                            numberOfTracks = cursorArtists.getInt(cursorArtists.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
                            artistID = cursorArtists.getLong(cursorArtists.getColumnIndex(MediaStore.Audio.Artists._ID));
                            numberOfAlbums = cursorArtists.getInt(cursorArtists.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
                            artists.add(new ArtistModel(artist, cover, artistKey, artistID, numberOfAlbums, numberOfTracks));
                            pos = cursorAlbumArt.getPosition();
                            break;
                        }

                    } while (cursorAlbumArt.moveToNext());
                }

                if (!foundCover) {
                    artist = cursorArtists.getString(cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                    artistKey = cursorArtists.getString(cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY));
                    numberOfTracks = cursorArtists.getInt(cursorArtists.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
                    artistID = cursorArtists.getLong(cursorArtists.getColumnIndex(MediaStore.Audio.Artists._ID));
                    numberOfAlbums = cursorArtists.getInt(cursorArtists.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
                    cover = null;
                    artists.add(new ArtistModel(artist, cover, artistKey, artistID, numberOfAlbums, numberOfTracks));
                }

            } while (cursorArtists.moveToNext());
        }

        // return new custom cursor
        Log.v(TAG, "finished");

        cursorAlbumArt.close();
        cursorArtists.close();
        return artists;
    }
}
