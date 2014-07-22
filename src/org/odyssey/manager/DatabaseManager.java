package org.odyssey.manager;

import java.util.ArrayList;
import java.util.Collections;

import org.odyssey.playbackservice.TrackItem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseManager {

    private PlaylistDBHelper mPlaylistDBHelper;
    private SQLiteDatabase mPlaylistDB;

    private String[] projectionTrackItems = { TrackItemTable.COLUMN_TRACKNUMBER, TrackItemTable.COLUMN_TRACKTITLE, TrackItemTable.COLUMN_TRACKALBUM, TrackItemTable.COLUMN_TRACKALBUMKEY, TrackItemTable.COLUMN_TRACKDURATION,
            TrackItemTable.COLUMN_TRACKARTIST, TrackItemTable.COLUMN_TRACKURL };

    public DatabaseManager(Context context) {
        mPlaylistDBHelper = new PlaylistDBHelper(context);
        mPlaylistDB = mPlaylistDBHelper.getWritableDatabase();
    }

    public void savePlaylist(ArrayList<TrackItem> playList) {
        // clear the database
        mPlaylistDB.delete(TrackItemTable.TABLE_NAME, null, null);

        ContentValues values = new ContentValues();

        mPlaylistDB.beginTransaction();

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

        mPlaylistDB.setTransactionSuccessful();
        mPlaylistDB.endTransaction();

    }

    public ArrayList<TrackItem> readPlaylist() {

        // get all trackitems from database and return them

        ArrayList<TrackItem> playList = new ArrayList<TrackItem>();

        Cursor cursor = mPlaylistDB.query(TrackItemTable.TABLE_NAME, projectionTrackItems, "", null, "", "", TrackItemTable.COLUMN_ID);

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKTITLE));
                long duration = cursor.getLong(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKDURATION));
                int no = cursor.getInt(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKNUMBER));
                String artist = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKARTIST));
                String album = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKALBUM));
                String url = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKURL));
                String albumKey = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKALBUMKEY));

                TrackItem item = new TrackItem(title, artist, album, url, no, duration, albumKey);

                playList.add(item);

            } while (cursor.moveToNext());
        }

        cursor.close();

        return playList;
    }

    public void clearPlaylist() {

        // clear the database
        mPlaylistDB.delete(TrackItemTable.TABLE_NAME, null, null);
    }

    public TrackItem getTrackItem(int id) {

        // get row id and return the trackitem

        String whereVal[] = { "" + id };

        Cursor cursor = mPlaylistDB.query(TrackItemTable.TABLE_NAME, projectionTrackItems, TrackItemTable.COLUMN_ID + "=?", whereVal, "", "", TrackItemTable.COLUMN_ID);

        TrackItem item = null;

        if (cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKTITLE));
            long duration = cursor.getLong(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKDURATION));
            int no = cursor.getInt(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKNUMBER));
            String artist = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKARTIST));
            String album = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKALBUM));
            String url = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKURL));
            String albumKey = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKALBUMKEY));

            item = new TrackItem(title, artist, album, url, no, duration, albumKey);
        }

        cursor.close();

        return item;
    }

    public int getSize() {

        // get number of rows in the database

        String[] projection = { TrackItemTable.COLUMN_ID };

        Cursor cursor = mPlaylistDB.query(TrackItemTable.TABLE_NAME, projection, "", null, "", "", TrackItemTable.COLUMN_ID);

        int size = cursor.getCount();

        cursor.close();

        return size;
    }

    public void enqueueTrackItem(TrackItem item) {

        // save trackitem to database

        ContentValues values = new ContentValues();

        mPlaylistDB.beginTransaction();

        // set trackitem parameters
        values.put(TrackItemTable.COLUMN_TRACKTITLE, item.getTrackTitle());
        values.put(TrackItemTable.COLUMN_TRACKDURATION, item.getTrackDuration());
        values.put(TrackItemTable.COLUMN_TRACKNUMBER, item.getTrackNumber());
        values.put(TrackItemTable.COLUMN_TRACKARTIST, item.getTrackArtist());
        values.put(TrackItemTable.COLUMN_TRACKALBUM, item.getTrackAlbum());
        values.put(TrackItemTable.COLUMN_TRACKURL, item.getTrackURL());
        values.put(TrackItemTable.COLUMN_TRACKALBUMKEY, item.getTrackAlbumKey());

        mPlaylistDB.insert(TrackItemTable.TABLE_NAME, null, values);

        mPlaylistDB.setTransactionSuccessful();
        mPlaylistDB.endTransaction();

    }

    public void enqueueTrackList(ArrayList<TrackItem> list) {

        // save trackitems to database

        ContentValues values = new ContentValues();

        mPlaylistDB.beginTransaction();

        for (TrackItem item : list) {

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

        mPlaylistDB.setTransactionSuccessful();
        mPlaylistDB.endTransaction();

    }

    public void dequeueTrackItem(int id) {

        // FIXME change ids of the rest

        // delete current row

        String whereVal[] = { "" + id };

        mPlaylistDB.delete(TrackItemTable.TABLE_NAME, TrackItemTable.COLUMN_ID + "=?", whereVal);

    }

    public void shufflePlaylist() {

        // TODO not very efficient

        // get all Tracks
        ArrayList<TrackItem> playList = new ArrayList<TrackItem>();

        Cursor cursor = mPlaylistDB.query(TrackItemTable.TABLE_NAME, projectionTrackItems, "", null, "", "", TrackItemTable.COLUMN_ID);

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKTITLE));
                long duration = cursor.getLong(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKDURATION));
                int no = cursor.getInt(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKNUMBER));
                String artist = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKARTIST));
                String album = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKALBUM));
                String url = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKURL));
                String albumKey = cursor.getString(cursor.getColumnIndex(TrackItemTable.COLUMN_TRACKALBUMKEY));

                TrackItem item = new TrackItem(title, artist, album, url, no, duration, albumKey);

                playList.add(item);

            } while (cursor.moveToNext());
        }
        cursor.close();

        // shuffle the list
        Collections.shuffle(playList);

        // update database
        ContentValues values = new ContentValues();

        mPlaylistDB.beginTransaction();

        for (int i = 0; i < playList.size(); i++) {
            values.clear();

            // set trackitem parameters
            values.put(TrackItemTable.COLUMN_TRACKTITLE, playList.get(i).getTrackTitle());
            values.put(TrackItemTable.COLUMN_TRACKDURATION, playList.get(i).getTrackDuration());
            values.put(TrackItemTable.COLUMN_TRACKNUMBER, playList.get(i).getTrackNumber());
            values.put(TrackItemTable.COLUMN_TRACKARTIST, playList.get(i).getTrackArtist());
            values.put(TrackItemTable.COLUMN_TRACKALBUM, playList.get(i).getTrackAlbum());
            values.put(TrackItemTable.COLUMN_TRACKURL, playList.get(i).getTrackURL());
            values.put(TrackItemTable.COLUMN_TRACKALBUMKEY, playList.get(i).getTrackAlbumKey());

            String whereVal[] = { "" + i };

            mPlaylistDB.update(TrackItemTable.TABLE_NAME, values, TrackItemTable.COLUMN_ID + "=?", whereVal);
        }

        mPlaylistDB.setTransactionSuccessful();
        mPlaylistDB.endTransaction();

    }

    public void playAllTracks() {
        // TODO
    }

    public void saveCurrentPlayState(long position, long trackNR, int random, int repeat) {
        // Delete old settings rows
        String whereStmt = SettingsTable.COLUMN_SETTINGSNAME + "=? OR " + SettingsTable.COLUMN_SETTINGSNAME + "=? OR " + SettingsTable.COLUMN_SETTINGSNAME + "=? OR " + SettingsTable.COLUMN_SETTINGSNAME + "=?";
        String[] whereArgs = { SettingsTable.TRACKNUMBER_ROW, SettingsTable.TRACKPOSITION_ROW, SettingsTable.RANDOM_STATE_ROW, SettingsTable.REPEAT_STATE_ROW };
        mPlaylistDB.delete(SettingsTable.TABLE_NAME, whereStmt, whereArgs);

        // Insert new values into table
        String positionStmt = "INSERT INTO " + SettingsTable.TABLE_NAME + " values ( \"" + SettingsTable.TRACKPOSITION_ROW + "\"," + "\"" + position + "\");";
        mPlaylistDB.execSQL(positionStmt);

        String nrStmt = "INSERT INTO " + SettingsTable.TABLE_NAME + " values ( \"" + SettingsTable.TRACKNUMBER_ROW + "\"," + "\"" + trackNR + "\");";
        mPlaylistDB.execSQL(nrStmt);

        String randomStmt = "INSERT INTO " + SettingsTable.TABLE_NAME + " values ( \"" + SettingsTable.RANDOM_STATE_ROW + "\"," + "\"" + random + "\");";
        mPlaylistDB.execSQL(randomStmt);

        String repeatStmt = "INSERT INTO " + SettingsTable.TABLE_NAME + " values ( \"" + SettingsTable.REPEAT_STATE_ROW + "\"," + "\"" + repeat + "\");";
        mPlaylistDB.execSQL(repeatStmt);
    }

    public long getLastTrackPosition() {
        String[] columns = { SettingsTable.COLUMN_SETTINGSVALUE };
        String selection = SettingsTable.COLUMN_SETTINGSNAME + "=?";
        String[] selectionArgs = { SettingsTable.TRACKPOSITION_ROW };
        Cursor resultCursor = mPlaylistDB.query(SettingsTable.TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
        if (resultCursor.moveToFirst()) {
            long value = resultCursor.getLong(resultCursor.getColumnIndex(SettingsTable.COLUMN_SETTINGSVALUE));
            return value;
        }
        return 0;
    }

    public long getLastTrackNumber() {
        String[] columns = { SettingsTable.COLUMN_SETTINGSVALUE };
        String selection = SettingsTable.COLUMN_SETTINGSNAME + "=?";
        String[] selectionArgs = { SettingsTable.TRACKNUMBER_ROW };
        Cursor resultCursor = mPlaylistDB.query(SettingsTable.TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
        if (resultCursor.moveToFirst()) {
            long value = resultCursor.getLong(resultCursor.getColumnIndex(SettingsTable.COLUMN_SETTINGSVALUE));
            return value;
        }
        return 0;
    }

    public int getLastRandomState() {
        String[] columns = { SettingsTable.COLUMN_SETTINGSVALUE };
        String selection = SettingsTable.COLUMN_SETTINGSNAME + "=?";
        String[] selectionArgs = { SettingsTable.RANDOM_STATE_ROW };
        Cursor resultCursor = mPlaylistDB.query(SettingsTable.TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
        if (resultCursor.moveToFirst()) {
            int value = resultCursor.getInt(resultCursor.getColumnIndex(SettingsTable.COLUMN_SETTINGSVALUE));
            return value;
        }
        return 0;
    }

    public int getLastRepeatState() {
        String[] columns = { SettingsTable.COLUMN_SETTINGSVALUE };
        String selection = SettingsTable.COLUMN_SETTINGSNAME + "=?";
        String[] selectionArgs = { SettingsTable.REPEAT_STATE_ROW };
        Cursor resultCursor = mPlaylistDB.query(SettingsTable.TABLE_NAME, columns, selection, selectionArgs, null, null, null, null);
        if (resultCursor.moveToFirst()) {
            int value = resultCursor.getInt(resultCursor.getColumnIndex(SettingsTable.COLUMN_SETTINGSVALUE));
            return value;
        }
        return 0;
    }
}
