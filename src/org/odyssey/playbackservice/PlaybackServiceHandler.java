package org.odyssey.playbackservice;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class PlaybackServiceHandler extends Handler {
    private static final String TAG = "OdysseyPlaybackServiceHandler";

    private final WeakReference<PlaybackService> mService;

    public PlaybackServiceHandler(Looper looper, PlaybackService service) {
        super(looper);
        Log.v(TAG, "Handler created");
        mService = new WeakReference<PlaybackService>(service);
        Log.v(TAG, "MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());
    }

    @Override
    public void handleMessage(Message msg) {
        Log.v(TAG, "handleMessage:" + msg);
        Log.v(TAG, "MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());
        super.handleMessage(msg);

        ControlObject msgObj = (ControlObject) msg.obj;

        // Check if object is received
        if (msgObj != null) {
            // Parse message
            if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAY) {
                mService.get().playURI(msgObj.getTrack());
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_STOP) {
                mService.get().stop();
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_PAUSE) {

            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_TOGGLEPAUSE) {
                mService.get().togglePause();
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_RESUME) {
                mService.get().resume();
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_NEXT) {
                mService.get().setNextTrack();
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_PREVIOUS) {
                mService.get().setPreviousTrack();
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_RANDOM) {
                mService.get().setRandom(msgObj.getBoolParam());
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_REPEAT) {
                mService.get().setRepeat(msgObj.getBoolParam());
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_SEEKTO) {
                mService.get().seekTo(msgObj.getIntParam());
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_JUMPTO) {
                mService.get().jumpToIndex(msgObj.getIntParam());
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACK) {

            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUEINDEX) {
                mService.get().dequeueTrack(msgObj.getIntParam());
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACKS) {

            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACK) {
                mService.get().enqueueTrack(msgObj.getTrack());
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYNEXT) {
                mService.get().enqueueAsNextTrack(msgObj.getTrack());
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACKS) {
                mService.get().enqueueTracks(msgObj.getTrackList());
            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_SETNEXTRACK) {

            } else if (msgObj.getAction() == ControlObject.PLAYBACK_ACTION.ODYSSEY_CLEARPLAYLIST) {
                mService.get().clearPlaylist();
            }

        }

    }

}
