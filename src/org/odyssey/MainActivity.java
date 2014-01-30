package org.odyssey;

import java.util.Locale;

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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements OnAlbumSelectedListener, OnArtistSelectedListener{
	
	private static final String TAG = "OdysseyMainActivity"; 
    
    private IOdysseyPlaybackService mPlaybackService;
    private ServiceConnection mConnection = null;
    
    
    private class PlaybackServiceConnection implements ServiceConnection {
    	@Override
    	public void onServiceConnected(ComponentName name, IBinder service) {
    		Log.v(TAG,"Service connection created");
    		Toast.makeText(MainActivity.this, "OdysseyPlaybackservice connected", Toast.LENGTH_LONG).show();
    		OdysseyApplication mainApplication = (OdysseyApplication) getApplication();
    		if ( mainApplication.getPlaybackService() == null ) { 
    			mainApplication.setPlaybackService(IOdysseyPlaybackService.Stub.asInterface(service));
    		}
    		mPlaybackService = mainApplication.getPlaybackService();
    	}
    	
    	@Override
    	public void onServiceDisconnected(ComponentName name) {
    		// TODO Auto-generated method stub
    		
    	}    	
    }
    

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
                .add(R.id.fragmentContainer, mArtistsAlbumsTabsFragment).commit();
        
        // create service connection
        Intent serviceStartIntent = new Intent(this,PlaybackService.class);
        startService(serviceStartIntent);
        mConnection = new PlaybackServiceConnection();
        bindService(new Intent(IOdysseyPlaybackService.class.getName()), 
        		mConnection, Context.BIND_AUTO_CREATE);
        
        OdysseyApplication mainApplication = (OdysseyApplication) getApplication();
        if ( mainApplication.getLibraryHelper() == null ) {
        	mainApplication.setLibraryHelper(new MusicLibraryHelper());
        }
        
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
        transaction.replace(R.id.fragmentContainer, newFragment);
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
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragmentContainer, newFragment);
        transaction.addToBackStack("ArtistFragment");

        // Commit the transaction
        transaction.commit();        	
	}	
	
	@Override
	public void onBackPressed() {
		android.support.v4.app.FragmentManager manager = getSupportFragmentManager();	
		
		super.onBackPressed();
			
	}
}
