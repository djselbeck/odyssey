package org.odyssey;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * This class is the parcelable which got send from the PlaybackService to notify
 * receivers like the main-GUI or possible later home screen widgets
 * 
 * PlaybackService --> NowPlayingInformation --> OdysseyApplication --> MainActivity
 * 											 |-> Homescreen Widget (later) 
 */

public final class NowPlayingInformation implements Parcelable{
	
	// Parcel data
	private int mPlaying;
	private String mPlayingURL;
	private int mPlayingIndex;
	
	
	static Parcelable.Creator<NowPlayingInformation> CREATOR = new Parcelable.Creator<NowPlayingInformation>() {

		@Override
		public NowPlayingInformation createFromParcel(Parcel source) {
			int playing = source.readInt();
			String playingURL = source.readString();
			int playingIndex = source.readInt();
			return new NowPlayingInformation(playing, playingURL,playingIndex);
		}

		@Override
		public NowPlayingInformation[] newArray(int size) {
			return new NowPlayingInformation[size];
		}
	};

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public NowPlayingInformation(int playing, String playingURL, int playingIndex) {
		mPlaying = playing;
		mPlayingURL = playingURL;
		mPlayingIndex = playingIndex;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mPlaying);
		dest.writeString(mPlayingURL);
	}
	
	public int getPlaying() {
		return mPlaying;
	}
	
	public String getPlayingURL() {
		return mPlayingURL;
	}
	
	public String toString() {
		return "Playing: " + mPlaying + " URL: " +  mPlayingURL;
	}

}
