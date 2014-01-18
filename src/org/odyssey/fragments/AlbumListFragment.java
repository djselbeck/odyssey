package org.odyssey.fragments;

import org.odyssey.MusicLibraryHelper;
import org.odyssey.OdysseyApplication;
import org.odyssey.R;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AlbumListFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.album_list_view, container, false);
	}

	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		OdysseyApplication mainApplication = (OdysseyApplication) activity
				.getApplication();

		MusicLibraryHelper helper = mainApplication.getLibraryHelper();

	}
}
