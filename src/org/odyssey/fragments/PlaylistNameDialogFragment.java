package org.odyssey.fragments;

import org.odyssey.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.EditText;

public class PlaylistNameDialogFragment extends DialogFragment {

    private String mPlaylistName;

    OnPlaylistNameListener mPlaylistNameSpecifiedCallback;

    public interface OnPlaylistNameListener {
        public void onPlaylistNameSpecified(String name);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mPlaylistNameSpecifiedCallback = (OnPlaylistNameListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPlaylistNameListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText editTextPlaylistName = new EditText(getActivity());
        editTextPlaylistName.setText("New Playlist");
        builder.setView(editTextPlaylistName);

        builder.setMessage(R.string.dialog_playlist_name).setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // accept playlist name
                mPlaylistName = editTextPlaylistName.getText().toString();
                mPlaylistNameSpecifiedCallback.onPlaylistNameSpecified(mPlaylistName);
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog dont create playlist
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
