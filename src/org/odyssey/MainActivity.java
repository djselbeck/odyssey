package org.odyssey;

import org.odyssey.fragments.AlbumsSectionFragment;
import org.odyssey.fragments.AlbumsSectionFragment.OnAlbumSelectedListener;
import org.odyssey.fragments.AlbumsTracksFragment;
import org.odyssey.fragments.ArtistsAlbumsTabsFragment;
import org.odyssey.fragments.ArtistsSectionFragment.OnArtistSelectedListener;
import org.odyssey.fragments.PlaylistFragment;
import org.odyssey.playbackservice.IOdysseyPlaybackService;
import org.odyssey.views.QuickControl;

import android.app.ActionBar;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends FragmentActivity implements OnAlbumSelectedListener, OnArtistSelectedListener {

	private static final String TAG = "OdysseyMainActivity";

	private IOdysseyPlaybackService mPlaybackService;

	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mNaviBarList;
	private String[] mNaviBarTitles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		// get Titles
		mNaviBarTitles = getResources().getStringArray(R.array.navibar_array);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mNaviBarList = (ListView) findViewById(R.id.left_drawer);

		// set a custom shadow that overlays the main content when the drawer
		// opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// set up the drawer's list view with items and click listener
		mNaviBarList.setAdapter(new ArrayAdapter<String>(this, R.layout.navibar_list_item, mNaviBarTitles));
		mNaviBarList.setOnItemClickListener(new NaviBarItemClickListener());

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();

		actionBar.setHomeButtonEnabled(true);
		// disable up home function
		actionBar.setDisplayHomeAsUpEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description for accessibility */
		R.string.drawer_close /* "close drawer" description for accessibility */
		) {
			public void onDrawerClosed(View view) {
				invalidateOptionsMenu();

			}

			public void onDrawerOpened(View view) {
				invalidateOptionsMenu();

			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {
			ArtistsAlbumsTabsFragment mArtistsAlbumsTabsFragment = new ArtistsAlbumsTabsFragment();

			// Add the fragment to the 'fragmentContainer' FrameLayout
			getSupportFragmentManager().beginTransaction().add(R.id.fragmentFrame, mArtistsAlbumsTabsFragment).commit();
		}

		OdysseyApplication mainApplication = (OdysseyApplication) getApplication();
		if (mainApplication.getLibraryHelper() == null) {
			mainApplication.setLibraryHelper(new MusicLibraryHelper());
		}

		// Register callbacks in mainapplication which currently manages
		// callback from playback service process
		QuickControl quickControl = (QuickControl) findViewById(R.id.quickControl);
		mainApplication.registerNowPlayingListener(quickControl);
		mPlaybackService = mainApplication.getPlaybackService();
	}

	@Override
	public void onAlbumSelected(String albumKey, String albumTitle, String albumCoverImagePath, String albumArtist) {

		mDrawerToggle.setDrawerIndicatorEnabled(false);
		// Create fragment and give it an argument for the selected article
		AlbumsTracksFragment newFragment = new AlbumsTracksFragment();
		Bundle args = new Bundle();
		args.putString(AlbumsTracksFragment.ARG_ALBUMKEY, albumKey);
		args.putString(AlbumsTracksFragment.ARG_ALBUMTITLE, albumTitle);
		args.putString(AlbumsTracksFragment.ARG_ALBUMART, albumCoverImagePath);
		args.putString(AlbumsTracksFragment.ARG_ALBUMARTIST, albumArtist);
		newFragment.setArguments(args);

		android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

		// Replace whatever is in the fragment_container view with this
		// fragment,
		// and add the transaction to the back stack so the user can navigate
		// back
		transaction.replace(R.id.fragmentFrame, newFragment);
		transaction.addToBackStack(null);

		// Commit the transaction
		transaction.commit();
	}

	@Override
	public void onArtistSelected(String artist, long artistID) {

		mDrawerToggle.setDrawerIndicatorEnabled(false);
		// Create fragment and give it an argument for the selected article
		AlbumsSectionFragment newFragment = new AlbumsSectionFragment();
		Bundle args = new Bundle();
		args.putString(AlbumsSectionFragment.ARG_ARTISTNAME, artist);
		args.putLong(AlbumsSectionFragment.ARG_ARTISTID, artistID);

		newFragment.setArguments(args);

		android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
		// Replace whatever is in the fragment_container view with this
		// fragment,
		// and add the transaction to the back stack so the user can navigate
		// back
		transaction.replace(R.id.fragmentFrame, newFragment);
		transaction.addToBackStack("ArtistFragment");

		// Commit the transaction
		transaction.commit();
	}

	@Override
	public void onBackPressed() {

		invalidateOptionsMenu();
		android.support.v4.app.FragmentManager manager = getSupportFragmentManager();

		super.onBackPressed();

		// enable navigation bar when backstack empty
		if (manager.getBackStackEntryCount() == 0) {
			mDrawerToggle.setDrawerIndicatorEnabled(true);
		}

	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		android.support.v4.app.FragmentManager manager = getSupportFragmentManager();

		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
		case android.R.id.home:
			if (manager.getBackStackEntryCount() > 0) {
				onBackPressed();
			} else {
				mDrawerToggle.setDrawerIndicatorEnabled(true);
				// The action bar home/up action should open or close the
				// drawer.
				// ActionBarDrawerToggle will take care of this.
				if (mDrawerToggle.onOptionsItemSelected(item)) {
					return true;
				}
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/* The click listner for ListView in the navigation drawer */
	private class NaviBarItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
			invalidateOptionsMenu();
			// TODO check clear backstack
			getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

			android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
			if (position == 0) {
				mDrawerToggle.setDrawerIndicatorEnabled(true);
				// FIXME always create a new fragment
				ArtistsAlbumsTabsFragment mArtistsAlbumsTabsFragment = new ArtistsAlbumsTabsFragment();
				// Replace whatever is in the fragment_container view with this
				// fragment,
				transaction.replace(R.id.fragmentFrame, mArtistsAlbumsTabsFragment);

				// Commit the transaction
				transaction.commit();
			} else if (position == 1) {
				mDrawerToggle.setDrawerIndicatorEnabled(true);

				PlaylistFragment mPlaylistFragment = new PlaylistFragment();
				// Replace whatever is in the fragment_container view with this
				// fragment,
				transaction.replace(R.id.fragmentFrame, mPlaylistFragment);

				// Commit the transaction
				transaction.commit();

				invalidateOptionsMenu();
			}

			// update selected item and title, then close the drawer
			mNaviBarList.setItemChecked(position, true);
			mDrawerLayout.closeDrawer(mNaviBarList);
		}
	}
}
