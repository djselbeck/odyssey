package org.odyssey;

import java.util.Locale;

import org.odyssey.MusicLibraryHelper.TrackItem;
import org.odyssey.fragments.AlbumsSectionFragment;
import org.odyssey.fragments.AlbumsSectionFragment.OnAlbumSelectedListener;
import org.odyssey.fragments.ArtistsSectionFragment.OnArtistSelectedListener;
import org.odyssey.fragments.AlbumsTracksFragment;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment;
import org.odyssey.fragments.ArtistsSectionFragment;
import org.odyssey.playbackservice.IOdysseyPlaybackService;
import org.odyssey.playbackservice.PlaybackService;


import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.LabeledIntent;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements OnAlbumSelectedListener, OnArtistSelectedListener{
	
	private static final String TAG = "OdysseyMainActivity"; 
    
    private IOdysseyPlaybackService mPlaybackService;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        
        if(savedInstanceState != null) {
        	return;
        }   
        
        ArtistsAlbumsTabsFragment mArtistsAlbumsTabsFragment = new ArtistsAlbumsTabsFragment();
        
        // Add the fragment to the 'fragmentContainer' FrameLayout
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentFrame, mArtistsAlbumsTabsFragment).commit();
        
        // Get placeholder frame for quickcontrols
        FrameLayout controlLayout = (FrameLayout)findViewById(R.id.controlLayout);
        
        // Create quickcontrol view from layout, add it to empty framelayout placeholder
        View controlView = getLayoutInflater().inflate(R.layout.quickcontrol_view, controlLayout); 
        
        // Set button listeners
        // FIXME CLEAN THE MESS UP
        controlView.findViewById(R.id.nextButton).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				OdysseyApplication app = (OdysseyApplication) getApplication();
				try {
					app.getPlaybackService().next();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
        
        controlView.findViewById(R.id.previousButton).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				OdysseyApplication app = (OdysseyApplication) getApplication();
				try {
					app.getPlaybackService().previous();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
        
        controlView.findViewById(R.id.playpauseButton).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				OdysseyApplication app = (OdysseyApplication) getApplication();
				try {
					app.getPlaybackService().togglePause();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
        
        // For later callback reference
        TextView nowPlayingTextView = (TextView)controlView.findViewById(R.id.titleView);
        ImageButton playpauseButton = (ImageButton)controlView.findViewById(R.id.playpauseButton);
        
                
        OdysseyApplication mainApplication = (OdysseyApplication) getApplication();
        if ( mainApplication.getLibraryHelper() == null ) {
        	mainApplication.setLibraryHelper(new MusicLibraryHelper());
        }
        
        // Register callbacks in mainapplication which currently manages callback from playback service process
        
        mainApplication.registerNowPlayingListener(new NowPlayingLabelListener(nowPlayingTextView));        
        mainApplication.registerNowPlayingListener(new NowPlayingPlayButtonListener(playpauseButton));
        mPlaybackService = mainApplication.getPlaybackService();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

	@Override
	public void onAlbumSelected(String albumKey, String albumTitle, String albumCoverImagePath, String albumArtist) {
		
        // update actionbar
        final ActionBar actionBar = getActionBar();

        actionBar.setHomeButtonEnabled(true);
        // allow backnavigation by homebutton 
        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);		

        // Create fragment and give it an argument for the selected article
    	AlbumsTracksFragment newFragment = new AlbumsTracksFragment();
        Bundle args = new Bundle();
        args.putString(AlbumsTracksFragment.ARG_ALBUMKEY, albumKey);
        args.putString(AlbumsTracksFragment.ARG_ALBUMTITLE, albumTitle);
        args.putString(AlbumsTracksFragment.ARG_ALBUMART, albumCoverImagePath);
        args.putString(AlbumsTracksFragment.ARG_ALBUMARTIST, albumArtist);
        newFragment.setArguments(args);    	

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragmentFrame, newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }
	
	@Override
	public void onArtistSelected(String artist, long artistID) {
		
        // update actionbar
        final ActionBar actionBar = getActionBar();

        actionBar.setHomeButtonEnabled(true);
        // allow backnavigation by homebutton 
        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);	
        
        // Create fragment and give it an argument for the selected article
        AlbumsSectionFragment newFragment = new AlbumsSectionFragment(); 
        Bundle args = new Bundle();
        args.putString(AlbumsSectionFragment.ARG_ARTISTNAME, artist);
        args.putLong(AlbumsSectionFragment.ARG_ARTISTID, artistID);
        
        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragmentFrame, newFragment);
        transaction.addToBackStack("ArtistFragment");

        // Commit the transaction
        transaction.commit();        	
	}	
	
	@Override
	public void onBackPressed() {
		android.support.v4.app.FragmentManager manager = getSupportFragmentManager();	
		
		super.onBackPressed();
			
	}
	
	
	// Listeners for NowPlaying receiving
	private class NowPlayingLabelListener implements OdysseyApplication.NowPlayingListener {
		private TextView mLabel;
		public NowPlayingLabelListener(TextView label ) {
			mLabel = label;
		}
		
		@Override
		public void onNewInformation(NowPlayingInformation info) {
			Log.v(TAG,"Received new label text info");
			final TrackItem trackItem = MusicLibraryHelper.getTrackItemFromURL(info.getPlayingURL(), getContentResolver());
			// Make sure listeners set GUI items only from GUI thread
			new Thread() {
		        public void run() {
		                runOnUiThread(new Runnable() {
						    @Override
						    public void run() {
						    	mLabel.setText( trackItem.trackTitle + " - " + trackItem.trackArtist );
						    }
						});
		        }
		    }.start();
		}
	}
	
	private class NowPlayingPlayButtonListener implements OdysseyApplication.NowPlayingListener {
		private ImageButton mButton;
		public NowPlayingPlayButtonListener(ImageButton button ) {
			mButton = button;
		}
		
		@Override
		public void onNewInformation(NowPlayingInformation info) {
			Log.v(TAG,"Received new label text info");
			final NowPlayingInformation tmpInfo = new NowPlayingInformation(info.getPlaying(),info.getPlayingURL());
			// Make sure listeners set GUI items only from GUI thread
			new Thread() {
		        public void run() {
		                runOnUiThread(new Runnable() {
						    @Override
						    public void run() {
						    	if(tmpInfo.getPlaying() == 0 ) {
						    		mButton.setImageResource(android.R.drawable.ic_media_play);
						    	} else if (tmpInfo.getPlaying() == 1 ) { 
						    		mButton.setImageResource(android.R.drawable.ic_media_pause);
						    	}
						    }
						});
		        }
		    }.start();	
		}
	}
}
