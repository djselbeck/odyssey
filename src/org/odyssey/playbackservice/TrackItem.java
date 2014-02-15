package org.odyssey.playbackservice;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class TrackItem implements Parcelable {
    private final static String TAG = "OdysseyTrackItem";
    private String mTrackTitle = null;
    private String mTrackAlbum = null;
    private long mTrackDuration = 0;
    private int mTrackNumber = 0;
    private String mTrackArtist = null;
    private String mTrackURL = null;
    private String mTrackAlbumKey = null;

    public String getTrackTitle() {
        return mTrackTitle;
    }

    public String getTrackAlbum() {
        return mTrackAlbum;
    }

    public long getTrackDuration() {
        return mTrackDuration;
    }

    public int getTrackNumber() {
        return mTrackNumber;
    }

    public String getTrackArtist() {
        return mTrackArtist;
    }

    public String getTrackURL() {
        return mTrackURL;
    }

    public String getTrackAlbumKey() {
        return mTrackAlbumKey;
    }

    public TrackItem(String title, String artist, String album, String url, int trackNo, long trackDuration, String albumKey) {
        mTrackTitle = title;
        mTrackArtist = artist;
        mTrackAlbum = album;
        mTrackURL = url;
        mTrackNumber = trackNo;
        mTrackDuration = trackDuration;
        mTrackAlbumKey = albumKey;
    }

    public TrackItem() {
        mTrackTitle = "";
        mTrackArtist = "";
        mTrackAlbum = "";
        mTrackURL = "";
        mTrackNumber = 0;
        mTrackDuration = 0;
        mTrackAlbumKey = "";
    }

    @Override
    public int describeContents() {
        return 1;
    }

    public String toString() {
        return "Title: " + mTrackTitle + " Artist: " + mTrackArtist + " URL: " + mTrackURL + " No.: " + mTrackNumber + " Duration(s): " + mTrackDuration;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTrackTitle);
        dest.writeString(mTrackArtist);
        dest.writeString(mTrackAlbum);
        dest.writeString(mTrackURL);
        dest.writeInt(mTrackNumber);
        dest.writeLong(mTrackDuration);
        dest.writeString(mTrackAlbumKey);
    }

    public static Parcelable.Creator<TrackItem> CREATOR = new Creator<TrackItem>() {

        @Override
        public TrackItem[] newArray(int size) {
            return new TrackItem[size];
        }

        @Override
        public TrackItem createFromParcel(Parcel source) {
            String title = source.readString();
            String artist = source.readString();
            String album = source.readString();
            String url = source.readString();
            int trackno = source.readInt();
            long duration = source.readLong();
            String albumKey = source.readString();

            TrackItem item = new TrackItem(title, artist, album, url, trackno, duration, albumKey);
            return item;
        }
    };

}
