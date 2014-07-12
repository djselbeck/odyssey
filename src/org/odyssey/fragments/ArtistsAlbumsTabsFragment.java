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

    // Listener for communication via container activity
    public interface OnAboutSelectedListener {
        public void onAboutSelected();
    }

    public interface OnSettingsSelectedListener {
        public void onSettingsSelected();
    }

    public interface OnPlayAllSelectedListener {
        public void OnPlayAllSelected();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // indicate this fragment has its own menu
        // setHasOptionsMenu(true);

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

        // set start page to albumsection
        mViewPager.setCurrentItem(1);

        return rootView;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the primary sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {
        Context mContext;
        static final int mNumberOfPages = 4;

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
            case 2:
                return new AllTracksFragment();
            case 3:
                return new AllPlaylistsFragment();
            default:
                return null;
            }
        }

        @Override
        public int getCount() {
            return mNumberOfPages;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return mContext.getText(R.string.section_title_artists);
            case 1:
                return mContext.getText(R.string.section_title_albums);
            case 2:
                return mContext.getText(R.string.section_title_alltracks);
            case 3:
                return mContext.getText(R.string.section_title_allplaylists);
            }
            return "";
        }
    }

}
