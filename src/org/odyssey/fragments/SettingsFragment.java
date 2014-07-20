package org.odyssey.fragments;

import org.odyssey.MainActivity;
import org.odyssey.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set visibility of quickcontrols
        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);

        // Set actionbar title
        getActivity().getActionBar().setTitle(R.string.settings_fragment_title);

        View rootView = inflater.inflate(R.layout.fragment_section_dummy, container, false);

        TextView text = (TextView) rootView.findViewById(android.R.id.text1);

        text.setText(R.string.dummy_section_text);

        return rootView;
    }
}
