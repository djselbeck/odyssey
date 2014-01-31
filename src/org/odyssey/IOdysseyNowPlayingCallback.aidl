package org.odyssey;

import org.odyssey.NowPlayingInformation;
// Declare parcable object with all the playing information

interface IOdysseyNowPlayingCallback {
	void receiveNewNowPlayingInformation(in NowPlayingInformation nowPlaying);
}