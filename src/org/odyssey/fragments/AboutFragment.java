package org.odyssey.fragments;

import org.odyssey.MainActivity;
import org.odyssey.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);
		
		View rootView = inflater.inflate(R.layout.fragment_about, container, false);
		
		TextView version = (TextView) rootView.findViewById(R.id.aboutFragmentVersion);
		
		version.setText("Alpha 0.5");
		
		TextView authors = (TextView) rootView.findViewById(R.id.aboutFragmentAuthors);
		
		authors.setText("Hendrik Borghorst"+"\n"+"Frederik Lütkes");
		
		TextView gitHub = (TextView) rootView.findViewById(R.id.aboutFragmentGitHub);
		
		gitHub.setText("https://github.com/djselbeck/odyssey");		
		
		return rootView;
	}
		
	
}
