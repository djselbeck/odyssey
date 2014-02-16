package org.odyssey.manager;

import java.util.ArrayList;

import org.odyssey.playbackservice.TrackItem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.provider.MediaStore;

public class PlaylistManager {

    private PlaylistDBHelper mPlaylistDBHelper;
    private SQLiteDatabase mPlaylistDB;

    private String[] projectionTrackItems = { TrackItemTable.COLUMN_TRACKNUMBER, TrackItemTable.COLUMN_TRACKTITLE, TrackItemTable.COLUMN_TRACKALBUM, TrackItemTable.COLUMN_TRACKALBUMKEY, TrackItemTable.COLUMN_TRACKDURATION,
            TrackItemTable.COLUMN_TRACKARTIST, TrackItemTable.COLUMN_TRACKURL };

    PlaylistManager(Context context) {
        mPlaylistDBHelper = new PlaylistDBHelper(context);
        mPlaylistDB = null;
    }

    public void savePlaylist(ArrayList<TrackItem> playList) {

        // save trackitems to database

        mPlaylistDB = mPlaylistDBHelper.getWritableDatabase();

        // clear the database
        mPlaylistDBHelper.onUpgrade(mPlaylistDB, 0, 0);

        ContentValues values = new ContentValues();

        for (TrackItem item : playList) {

            values.clear();

            // set trackitem parameters
            values.put(TrackItemTable.COLUMN_TRACKTITLE, item.getTrackTitle());
            values.put(TrackItemTable.COLUMN_TRACKDURATION, item.getTrackDuration());
            values.put(TrackItemTable.COLUMN_TRACKNUMBER, item.getTrackNumber());
            values.put(TrackItemTable.COLUMN_TRACKARTIST, item.getTrackArtist());
            values.put(TrackItemTable.COLUMN_TRACKALBUM, item.getTrackAlbum());
            values.put(TrackItemTable.COLUMN_TRACKURL, item.getTrackURL());
            values.put(TrackItemTable.COLUMN_TRACKALBUMKEY, item.getTrackAlbumKey());

            mPlaylistDB.insert(TrackItemTable.TABLE_NAME, null, values);
        }

        mPlaylistDBHelper.close();
    }

    public ArrayList<TrackItem> readPlaylist() {

        // get all trackitems from database and return them

        mPlaylistDB = mPlaylistDBHelper.getWritableDatabase();

        ArrayList<TrackItem> playList = new ArrayList<TrackItem>();

        Cursor cursor = mPlaylistDB.query(TrackItemTable.TABLE_NAME, projectionTrackItems, "", null, "", "", TrackItemTable.COLUMN_ID);

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String albumKey = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKALBUMKEY));

                TrackItem item = new TrackItem(title, artist, album, url, no, duration, albumKey);

                playList.add(item);

            } while (cursor.moveToNext());
        }

        cursor.close();

        mPlaylistDBHelper.close();

        return playList;
    }
}
