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
	
	
	static Parcelable.Creator<NowPlayingInformation> CREATOR = new Parcelable.Creator<NowPlayingInformation>() {

		@Override
		public NowPlayingInformation createFromParcel(Parcel source) {
			int playing = source.readInt();
			String playingURL = source.readString();
			return new NowPlayingInformation(playing, playingURL);
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
	
	public NowPlayingInformation(int playing, String playingURL) {
		mPlaying = playing;
		mPlayingURL = playingURL;
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
