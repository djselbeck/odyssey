package org.odyssey;

import java.util.Locale;

import org.odyssey.fragments.AlbumsSectionFragment;
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
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
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

public class MainActivity extends FragmentActivity implements TabListener {
	
	private static final String TAG = "OdysseyMainActivity";


    /**
     * The {@link android.support.v4.view.ViewPager} that will display the object collection.
     */
    ViewPager mViewPager;
    
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * three primary sections of the app. We use a {@link android.support.v4.app.FragmentPagerAdapter}
     * derivative, which will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;    
    
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
        
        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager(),this);        
        
        // Set up the action bar.
        final ActionBar actionBar = getActionBar();

        // Specify that the Home/Up button should not be enabled, since there is no hierarchical
        // parent.
        actionBar.setHomeButtonEnabled(false);

        // Specify that we will be displaying tabs in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }        
        
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
    
    private OnClickListener mPlayListener = new OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			OdysseyApplication mainApplication = (OdysseyApplication) getApplication();
			mainApplication.getLibraryHelper().getAlbums(getContentResolver());
		}
	};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {
    	Context mContext;

        public AppSectionsPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            this.mContext = context;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                	return new ArtistsSectionFragment();
                case 1:
                	return new AlbumsSectionFragment();                    
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch ( position ) {
            case 0: 
            	return mContext.getText(R.string.section_title_artists);
            case 1:
            	return mContext.getText(R.string.section_title_albums);
            }
            return "";
        }
    }


	@Override
	public void onTabReselected(Tab tab, FragmentTransaction transaction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction transaction) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
		// TODO Auto-generated method stub
		
	}   


}
