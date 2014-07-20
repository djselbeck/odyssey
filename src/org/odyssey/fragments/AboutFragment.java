package org.odyssey.fragments;

import org.odyssey.MainActivity;
import org.odyssey.R;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AboutFragment extends Fragment {

    private int mEasterEgg = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set visibility of quickcontrols
        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);

        // Set actionbar title
        getActivity().getActionBar().setTitle(R.string.about_fragment_title);

        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        final TextView easterEgg = (TextView) rootView.findViewById(R.id.aboutFragmentEasterEgg);

        TextView version = (TextView) rootView.findViewById(R.id.aboutFragmentVersion);

        String versionName = "";
        // get version from manifest
        try {
            versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        version.setText(versionName);

        TextView gitHub = (TextView) rootView.findViewById(R.id.aboutFragmentGitHub);

        gitHub.setText("https://github.com/djselbeck/odyssey");

        Linkify.addLinks(gitHub, Linkify.ALL);

        ImageView appIcon = (ImageView) rootView.findViewById(R.id.aboutFragmentImageView);

        appIcon.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                mEasterEgg++;
                if (mEasterEgg > 2) {
                    mEasterEgg = 0;
                }

                switch (mEasterEgg) {
                case 0:
                    easterEgg.setText("");
                    break;
                case 1:
                    easterEgg.setText(R.string.easterEgg1);
                    break;
                case 2:
                    easterEgg.setText(R.string.easterEgg2);
                    break;
                default:
                    break;
                }
            }

        });

        return rootView;
    }

}
