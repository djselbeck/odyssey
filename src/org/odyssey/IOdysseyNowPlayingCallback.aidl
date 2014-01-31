package org.odyssey;
//Interface specification
import org.odyssey.NowPlayingInformation;
// Declare parcable object with all the playing information

interface IOdysseyNowPlayingCallback {
	void receiveNewNowPlayingInformation(in NowPlayingInformation nowPlaying);
}