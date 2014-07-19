package org.odyssey.databasemodel;

public class ArtistModel {

    private String mArtistName;
    private String mArtURL;
    private String mArtistKey;
    private int mAlbumCount;
    private int mTrackCount;
    private long mID;

    public ArtistModel(String name, String artURL, String artistKey, long artistID, int albumCount, int trackCount) {
        mArtistName = name;
        mArtURL = artURL;
        mArtistKey = artistKey;
        mAlbumCount = albumCount;
        mTrackCount = trackCount;
        mID = artistID;
    }

    public String getArtURL() {
        return mArtURL;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public String getArtistKey() {
        return mArtistKey;
    }

    public long getID() {
        return mID;
    }

    @Override
    public String toString() {
        return "Artist: " + getArtistName();
    }
}
