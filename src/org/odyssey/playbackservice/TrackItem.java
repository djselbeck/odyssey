package org.odyssey.playbackservice;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackItem implements Parcelable {
	private String mTrackTitle;
	private String mTrackAlbum;
	private long mTrackDuration;
	private int mTrackNumber;
	private String mTrackArtist;
	private String mTrackURL;
	
	public static void setCREATOR(Parcelable.Creator<TrackItem> cREATOR) {
		CREATOR = cREATOR;
	}


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

	public TrackItem(String title, String artist, String album, String url, int trackNo, long trackDuration ) {
		mTrackTitle = title;
		mTrackArtist = artist;
		mTrackAlbum = album;
		mTrackURL = url;
		mTrackNumber = trackNo;
		mTrackDuration = trackDuration;
	}
	
	
	@Override
	public int describeContents() {
		return 0;
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
	}

	static Parcelable.Creator<TrackItem> CREATOR = new Creator<TrackItem>() {
		
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
			long duration = source.readInt();
			return new TrackItem(title, artist, album, url, trackno, duration);
		}
	};

}
