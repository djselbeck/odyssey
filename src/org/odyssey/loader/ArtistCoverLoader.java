package org.odyssey.loader;

import java.util.ArrayList;
import java.util.List;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.databasemodel.ArtistModel;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;

/*
 * Custom Loader for ARTIST with ALBUM_ART
 */
public class ArtistCoverLoader extends AsyncTaskLoader<List<ArtistModel>> {

    private static final String TAG = "OdysseyArtistLoader";

    Context mContext;

    public ArtistCoverLoader(Context context) {
        super(context);
        this.mContext = context;

        // Starts loading of the backgroundData
        forceLoad();
    }

    @Override
    public List<ArtistModel> loadInBackground() {

        // get all album covers
        Cursor cursorAlbumArt = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM }, "", null,
                MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE");

        // get all artists
        Cursor cursorArtists = mContext.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, "", null, MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE");

        ArrayList<ArtistModel> artists = new ArrayList<ArtistModel>();

        // join both cursor if match is found
        String artist, artistKey, coverPath, albumArtist, albumCoverPath;
        int numberOfTracks, numberOfAlbums;
        long artistID;
        boolean foundCover = false;
        int pos = 0;

        int artistTitleColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
        int artistKeyColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY);
        int artistNoTColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
        int artistIDColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists._ID);
        int artistNoAColumnIndex = cursorArtists.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);

        int albumArtistTitleColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
        int albumCoverPathColumnIndex = cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);

        CursorJoiner cursorArtistsWithArt = new CursorJoiner(cursorAlbumArt, new String[] { MediaStore.Audio.Albums.ARTIST }, cursorArtists, new String[] { MediaStore.Audio.Artists.ARTIST });

        for (CursorJoiner.Result result : cursorArtistsWithArt) {
            switch (result) {
            case LEFT:
                // handle case where a row in cursorAlbumArt is unique
                // this case should never occur
                break;
            case RIGHT:
                // handle case where a row in cursorArtists is unique
                artist = cursorArtists.getString(artistTitleColumnIndex);
                artistKey = cursorArtists.getString(artistKeyColumnIndex);
                numberOfTracks = cursorArtists.getInt(artistNoTColumnIndex);
                artistID = cursorArtists.getLong(artistIDColumnIndex);
                numberOfAlbums = cursorArtists.getInt(artistNoAColumnIndex);
                coverPath = null;
                artists.add(new ArtistModel(artist, coverPath, artistKey, artistID, numberOfAlbums, numberOfTracks));
                break;
            case BOTH:
                // handle case where a row with the same key is in both cursors
                artist = cursorArtists.getString(artistTitleColumnIndex);
                artistKey = cursorArtists.getString(artistKeyColumnIndex);
                numberOfTracks = cursorArtists.getInt(artistNoTColumnIndex);
                artistID = cursorArtists.getLong(artistIDColumnIndex);
                numberOfAlbums = cursorArtists.getInt(artistNoAColumnIndex);
                coverPath = cursorAlbumArt.getString(albumCoverPathColumnIndex);
                artists.add(new ArtistModel(artist, coverPath, artistKey, artistID, numberOfAlbums, numberOfTracks));
                break;
            }
        }

        // if (cursorArtists.moveToFirst()) {
        // do {
        // artist = cursorArtists.getString(artistTitleColumnIndex);
        // artistKey = cursorArtists.getString(artistKeyColumnIndex);
        // numberOfTracks = cursorArtists.getInt(artistNoTColumnIndex);
        // artistID = cursorArtists.getLong(artistIDColumnIndex);
        // numberOfAlbums = cursorArtists.getInt(artistNoAColumnIndex);
        // coverPath = null;
        //
        // if (cursorAlbumArt.moveToPosition(pos)) {
        // foundCover = false;
        // // search for cover match
        // do {
        // albumArtist = cursorAlbumArt.getString(albumArtistTitleColumnIndex);
        // albumCoverPath = cursorAlbumArt.getString(albumCoverPathColumnIndex);
        //
        // if (artist.equals(albumArtist) && albumCoverPath != null &&
        // !albumCoverPath.equals("")) {
        // // artist and album cover match
        // foundCover = true;
        //
        // coverPath = albumCoverPath;
        // pos = cursorAlbumArt.getPosition();
        // }
        //
        // } while (cursorAlbumArt.moveToNext() && !foundCover);
        // }
        //
        // artists.add(new ArtistModel(artist, coverPath, artistKey, artistID,
        // numberOfAlbums, numberOfTracks));
        //
        // } while (cursorArtists.moveToNext());
        // }

        // return new custom cursor

        cursorAlbumArt.close();
        cursorArtists.close();
        return artists;
    }
}
