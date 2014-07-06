package org.odyssey.widget;

import java.util.ArrayList;

import org.odyssey.MainActivity;
import org.odyssey.NowPlayingInformation;
import org.odyssey.R;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.TrackItem;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.widget.RemoteViews;

public class OdysseyAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        super.onUpdate(context, appWidgetManager, appWidgetIds);

        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this
        // provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // get remoteviews
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.odyssey_appwidget);

            // Main action
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.putExtra("Fragment", "currentsong");
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);
            views.setOnClickPendingIntent(R.id.odysseyWidgetImageView, mainPendingIntent);

            // Play/Pause action
            Intent playPauseIntent = new Intent(PlaybackService.ACTION_TOGGLEPAUSE);
            PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(context, 42, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.odysseyWidgetPlaypauseButton, playPausePendingIntent);

            // Previous song action
            Intent prevIntent = new Intent(PlaybackService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(context, 42, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.odysseyWidgetPreviousButton, prevPendingIntent);

            // Next song action
            Intent nextIntent = new Intent(PlaybackService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 42, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.odysseyWidgetNextButton, nextPendingIntent);

            // Quit action
            Intent stopIntent = new Intent(PlaybackService.ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 42, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.odysseyWidgetStopButton, stopPendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app
            // widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        // get remoteviews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.odyssey_appwidget);

        if (intent.getAction().equals(PlaybackService.MESSAGE_NEWTRACKINFORMATION)) {

            intent.setExtrasClassLoader(context.getClassLoader());

            ArrayList<Parcelable> trackItemList = intent.getParcelableArrayListExtra(PlaybackService.INTENT_TRACKITEMNAME);

            if (trackItemList.size() == 1) {
                TrackItem item = (TrackItem) trackItemList.get(0);

                views.setTextViewText(R.id.odysseyWidgetTextViewTrack, item.getTrackTitle());
                views.setTextViewText(R.id.odysseyWidgetTextViewArtist, item.getTrackArtist());

                String where = android.provider.MediaStore.Audio.Albums.ALBUM_KEY + "=?";

                String whereVal[] = { item.getTrackAlbumKey() };

                Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART }, where, whereVal, "");

                String coverPath = null;
                if (cursor.moveToFirst()) {
                    coverPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                }

                if (coverPath != null) {
                    BitmapDrawable cover = (BitmapDrawable) BitmapDrawable.createFromPath(coverPath);

                    views.setImageViewBitmap(R.id.odysseyWidgetImageView, cover.getBitmap());
                } else {
                    views.setImageViewResource(R.id.odysseyWidgetImageView, R.drawable.coverplaceholder);
                }
            }

            ArrayList<Parcelable> infoList = intent.getParcelableArrayListExtra(PlaybackService.INTENT_NOWPLAYINGNAME);

            if (infoList.size() == 1) {
                NowPlayingInformation info = (NowPlayingInformation) infoList.get(0);

                if (info.getPlaying() == 0) {
                    // Show play icon
                    views.setImageViewResource(R.id.odysseyWidgetPlaypauseButton, android.R.drawable.ic_media_play);
                } else if (info.getPlaying() == 1) {
                    // Show pause icon
                    views.setImageViewResource(R.id.odysseyWidgetPlaypauseButton, android.R.drawable.ic_media_pause);
                }
            }
        }

        // TODO is there a better way?
        // reset button actions
        // Main action
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);
        views.setOnClickPendingIntent(R.id.odysseyWidgetImageView, mainPendingIntent);

        // Play/Pause action
        Intent playPauseIntent = new Intent(PlaybackService.ACTION_TOGGLEPAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(context, 42, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.odysseyWidgetPlaypauseButton, playPausePendingIntent);

        // Previous song action
        Intent prevIntent = new Intent(PlaybackService.ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(context, 42, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.odysseyWidgetPreviousButton, prevPendingIntent);

        // Next song action
        Intent nextIntent = new Intent(PlaybackService.ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 42, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.odysseyWidgetNextButton, nextPendingIntent);

        // Quit action
        Intent stopIntent = new Intent(PlaybackService.ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 42, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.odysseyWidgetStopButton, stopPendingIntent);

        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, OdysseyAppWidgetProvider.class), views);
    }
}
