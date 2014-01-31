package org.odyssey.fragments;

import org.odyssey.R;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ArtistsAlbumsTabsFragment extends Fragment {

	ViewPager mViewPager;
	AppSectionsPagerAdapter mAppSectionsPagerAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View rootView = inflater.inflate(R.layout.fragment_artists_albums_tabs, container, false);

		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getChildFragmentManager(), getActivity());

		// Set up the ViewPager, attaching the adapter and setting up a listener
		// for when the
		// user swipes between sections.
		mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);

		return rootView;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
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
			switch (position) {
			case 0:
				return mContext.getText(R.string.section_title_artists);
			case 1:
				return mContext.getText(R.string.section_title_albums);
			}
			return "";
		}
	}

}
