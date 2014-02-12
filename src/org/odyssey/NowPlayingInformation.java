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

public final class NowPlayingInformation implements Parcelable {

    // Parcel data
    private int mPlaying;
    private String mPlayingURL;
    private int mPlayingIndex;
    private int mRepeat;
    private int mRandom;

    public static Parcelable.Creator<NowPlayingInformation> CREATOR = new Parcelable.Creator<NowPlayingInformation>() {

        @Override
        public NowPlayingInformation createFromParcel(Parcel source) {
            int playing = source.readInt();
            String playingURL = source.readString();
            int playingIndex = source.readInt();
            int repeat = source.readInt();
            int random = source.readInt();

            return new NowPlayingInformation(playing, playingURL, playingIndex, repeat, random);
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

    public NowPlayingInformation(int playing, String playingURL, int playingIndex, int repeat, int random) {
        mPlaying = playing;
        mPlayingURL = playingURL;
        mPlayingIndex = playingIndex;
        mRepeat = repeat;
        mRandom = random;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPlaying);
        dest.writeString(mPlayingURL);
        dest.writeInt(mPlayingIndex);
        dest.writeInt(mRepeat);
        dest.writeInt(mRandom);
    }

    public int getPlaying() {
        return mPlaying;
    }

    public String getPlayingURL() {
        return mPlayingURL;
    }

    public String toString() {
        return "Playing: " + mPlaying + " URL: " + mPlayingURL + " index: " + mPlayingIndex + "repeat: " + mRepeat + "random: " + mRandom;
    }

    public int getPlayingIndex() {
        return mPlayingIndex;
    }

    public int getRepeat() {
        return mRepeat;
    }

    public int getRandom() {
        return mRandom;
    }

}
