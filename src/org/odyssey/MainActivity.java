package org.odyssey;

import java.util.Locale;

import org.odyssey.playbackservice.IOdysseyPlaybackService;
import org.odyssey.playbackservice.PlaybackService;


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

public class MainActivity extends FragmentActivity {
	
	private static final String TAG = "OdysseyMainActivity";


    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    
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
        
        Intent serviceStartIntent = new Intent(this,PlaybackService.class);
        startService(serviceStartIntent);
        mConnection = new PlaybackServiceConnection();
        bindService(new Intent(IOdysseyPlaybackService.class.getName()), 
        		mConnection, Context.BIND_AUTO_CREATE);
        
        OdysseyApplication mainApplication = (OdysseyApplication) getApplication();
        if ( mainApplication.getLibraryHelper() == null ) {
        	mainApplication.setLibraryHelper(new MusicLibraryHelper());
        }
        
        Button playBtn = (Button)findViewById(R.id.button1);
        playBtn.setOnClickListener(mPlayListener);
        
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
    
    


}
