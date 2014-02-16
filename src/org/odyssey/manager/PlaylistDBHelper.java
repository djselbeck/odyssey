package org.odyssey.manager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PlaylistDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "PlaylistDB";
    public static final int DATABASE_VERSION = 1;

    public PlaylistDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        TrackItemTable.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TrackItemTable.onUpgrade(db, oldVersion, newVersion);
    }
}
