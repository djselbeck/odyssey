package org.odyssey.fragments;

import java.util.ArrayList;

import org.odyssey.MainActivity;
import org.odyssey.NowPlayingInformation;
import org.odyssey.R;
import org.odyssey.playbackservice.IOdysseyPlaybackService;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.TrackItem;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistFragment extends Fragment {

    private static final String TAG = "OdysseyPlaylistFragment";
    private int mPlayingIndex = 0;
    private ListView mListView = null;

    private PlaylistTracksAdapter mPlayListAdapter;

    private PlaybackServiceConnection mServiceConnection = null;
    private NowPlayingReceiver mNowPlayingReceiver = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set visibility of quickcontrols
        ((MainActivity) getActivity()).getQuickControl().setVisibility(View.VISIBLE);

        // Set actionbar title
        getActivity().getActionBar().setTitle(R.string.playlist_fragment_title);

        // indicate this fragment has its own menu
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);

        // create listview for tracklist
        mListView = (ListView) rootView.findViewById(R.id.listViewPlaylist);

        mListView.setAdapter(mPlayListAdapter);

        mListView.setOnItemClickListener(new OnItemClickListener() {

            // FIXME temporary just play clicked song
            @Override
            public void onItemClick(AdapterView<?> arg0, View viewItem, int position, long id) {

                // Get main application object for service connection

                try {
                    mServiceConnection.getPBS().jumpTo(position);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });

        registerForContextMenu(mListView);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister registered broadcast receiver to save some resources
        if (mNowPlayingReceiver != null) {
            getActivity().getApplicationContext().unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }
        mServiceConnection.closeConnection();
        mServiceConnection = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mServiceConnection = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register receiver to get newest information
        if (mNowPlayingReceiver != null) {
            getActivity().getApplicationContext().unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }
        mNowPlayingReceiver = new NowPlayingReceiver();
        getActivity().getApplicationContext().registerReceiver(mNowPlayingReceiver, new IntentFilter(PlaybackService.MESSAGE_NEWTRACKINFORMATION));

        // Reopen service connection
        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.setNotifier(new ServiceListener());
        mServiceConnection.openConnection();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.playlist_actionbar_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_clearplaylist:
            mPlayListAdapter.clear();
            return true;
        case R.id.action_saveplaylist:
            openPlaylistNameDialog();
            return true;
        case R.id.action_jumpcurrent:
            mListView.setSelection(mPlayingIndex);
            return true;
        case R.id.action_shuffleplaylist:
            try {
                mServiceConnection.getPBS().shufflePlaylist();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mPlayListAdapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class PlaylistTracksAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        private IOdysseyPlaybackService mPlaybackService;

        public PlaylistTracksAdapter(IOdysseyPlaybackService iOdysseyPlaybackService) {
            super();

            mInflater = getLayoutInflater(getArguments());
            mPlaybackService = iOdysseyPlaybackService;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TextView trackTitleView;
            TextView trackDurationView;
            TextView trackNumberView;
            TextView trackArtistView;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.listview_playlist_item, null);
            }

            trackTitleView = (TextView) convertView.findViewById(R.id.textViewPlaylistTitleItem);
            trackDurationView = (TextView) convertView.findViewById(R.id.textViewPlaylistDurationItem);
            trackNumberView = (TextView) convertView.findViewById(R.id.textViewPlaylistNumberItem);
            trackArtistView = (TextView) convertView.findViewById(R.id.textViewPlaylistArtistItem);

            // set tracktitle
            TrackItem trackItem;
            try {
                trackItem = mPlaybackService.getPlaylistSong(position);
            } catch (RemoteException e) {
                trackItem = new TrackItem();
            }

            trackTitleView.setText(trackItem.getTrackTitle());

            // calculate duration in minutes and seconds
            String seconds = String.valueOf((trackItem.getTrackDuration() % 60000) / 1000);

            String minutes = String.valueOf(trackItem.getTrackDuration() / 60000);

            if (seconds.length() == 1) {
                trackDurationView.setText(minutes + ":0" + seconds);
            } else {
                trackDurationView.setText(minutes + ":" + seconds);
            }

            // calculate track and discnumber
            if (("" + trackItem.getTrackNumber()).length() < 4) {
                trackNumberView.setText("" + trackItem.getTrackNumber());
            } else {

                // TODO shall we use discnumber?
                String discNumber = ("" + trackItem.getTrackNumber()).substring(0, 2);
                String trackNumber = ("" + trackItem.getTrackNumber()).substring(2);

                trackNumberView.setText(trackNumber);
            }

            // set artist
            trackArtistView.setText(trackItem.getTrackArtist() + " - " + trackItem.getTrackAlbum());

            if (position == mPlayingIndex) {
                ImageView playImage = (ImageView) convertView.findViewById(R.id.imageViewPlaylistPlay);

                playImage.setVisibility(ImageView.VISIBLE);
            } else {
                ImageView playImage = (ImageView) convertView.findViewById(R.id.imageViewPlaylistPlay);

                playImage.setVisibility(ImageView.GONE);
            }

            return convertView;

        }

        @Override
        public int getCount() {
            try {
                if (mPlaybackService != null) {
                    return mPlaybackService.getPlaylistSize();
                } else {
                    return 0;
                }
            } catch (RemoteException e) {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            try {
                if (mPlaybackService != null) {
                    return mPlaybackService.getPlaylistSong(position);
                } else {
                    return null;
                }
            } catch (RemoteException e) {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            // FIXME for now just return positon as id
            return position;
        }

        public void remove(int position) {
            try {
                mPlaybackService.dequeueTrackIndex(position);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            notifyDataSetChanged();
        }

        public void clear() {
            try {
                mPlaybackService.clearPlaylist();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            notifyDataSetChanged();
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.playlist_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.playlist_context_menu_action_remove:
            mPlayListAdapter.remove(info.position);
            return true;
        case R.id.playlist_context_menu_action_playnext:
            TrackItem track = (TrackItem) mListView.getAdapter().getItem(info.position);
            mPlayListAdapter.remove(info.position);
            try {
                mServiceConnection.getPBS().enqueueTrackAsNext(track);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    private void updateSelectionInGUI() {
        Activity activity = (Activity) getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListView.setSelection(mPlayingIndex);
                    mPlayListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class NowPlayingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.MESSAGE_NEWTRACKINFORMATION)) {
                Log.v(TAG, "Received new information");
                // Extract nowplaying info
                ArrayList<NowPlayingInformation> infoArray = intent.getExtras().getParcelableArrayList(PlaybackService.INTENT_NOWPLAYINGNAME);
                if (infoArray.size() != 0) {
                    NowPlayingInformation info = infoArray.get(0);
                    mPlayingIndex = info.getPlayingIndex();
                    updateSelectionInGUI();
                }
            }
        }
    }

    private class ServiceListener implements PlaybackServiceConnection.ConnectionNotifier {

        @Override
        public void onConnect() {
            Log.v(TAG, "Service connected :)");
            // Set index of current song
            try {
                mPlayingIndex = mServiceConnection.getPBS().getCurrentIndex();
                mPlayListAdapter = new PlaylistTracksAdapter(mServiceConnection.getPBS());
                mListView.setAdapter(mPlayListAdapter);

                mPlayListAdapter.notifyDataSetChanged();
                updateSelectionInGUI();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnect() {
            Log.v(TAG, "Service disconnected :'(");
        }

    }

    private void openPlaylistNameDialog() {

        PlaylistNameDialogFragment dlg = new PlaylistNameDialogFragment();

        dlg.show(getActivity().getSupportFragmentManager(), "PlaylistNameDialog");
    }

    public void savePlaylist(String playlistName) {

        // call pbs and save current playlist to mediastore
        try {
            mServiceConnection.getPBS().savePlaylist(playlistName);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
