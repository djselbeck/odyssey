package org.odyssey.manager;

import org.odyssey.MusicLibraryHelper;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public class ArtistCoverLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = "ArtistCoverLoader";

    Context mContext;

    public ArtistCoverLoader(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public Cursor loadInBackground() {
        Log.v(TAG, "load ArtistCovers");

        // TODO still unfinished

        // get all albums
        String[] projectionArgs = { MediaStore.Audio.Albums.ALBUM_ART, android.provider.MediaStore.Audio.Albums.ARTIST };

        Cursor albumArtCursor = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projectionArgs, "", null, MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE");

        Cursor artistsCursor = mContext.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionArtists, "", null, MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE");

        return null;
    }
}
