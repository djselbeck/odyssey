package org.odyssey.databasemodel;

public class AlbumModel {

    private String mAlbumName;
    private String mAlbumArtURL;
    private String mArtistName;
    private String mAlbumKey;
    
    public AlbumModel(String name, String albumArtURL, String artistName, String albumkey ) {
        mAlbumName = name;
        mAlbumArtURL = albumArtURL;
        mArtistName = artistName;
        mAlbumKey = albumkey;
    }
    
    public String getAlbumName() {
        return mAlbumName;
    }
    
    public String getAlbumArtURL() {
        return mAlbumArtURL;
    }
    
    public String getArtistName() {
        return mArtistName;
    }
    
    public String getAlbumKey() {
        return mAlbumKey;
    }
    
    @Override
    public String toString() {
        return "Album: " + getAlbumName() + " from: " + getArtistName();
    }
}
