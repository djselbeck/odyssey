package org.odyssey.manager;

import android.database.sqlite.SQLiteDatabase;

public class SettingsTable {

    // Settings table
    public static final String TABLE_NAME = "odyssey_settings";
    public static final String COLUMN_SETTINGSNAME = "tracknumber";
    public static final String COLUMN_SETTINGSVALUE = "title";
    
    public static final String TRACKNUMBER_ROW = "tracknumber";
    public static final String TRACKPOSITION_ROW = "trackposition";
    


    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table if not exists " + TABLE_NAME + "(" + COLUMN_SETTINGSNAME + " text, " + COLUMN_SETTINGSVALUE + " text);";

    public static void onCreate(SQLiteDatabase database) {
        // create new table
        database.execSQL(DATABASE_CREATE);
    }
}
