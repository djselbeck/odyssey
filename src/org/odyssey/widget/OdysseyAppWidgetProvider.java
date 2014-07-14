package org.odyssey.widget;

import java.util.ArrayList;

import org.odyssey.MainActivity;
import org.odyssey.MusicLibraryHelper;
import org.odyssey.MusicLibraryHelper.CoverBitmapGenerator;
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
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcelable;
import android.util.Log;
import android.widget.RemoteViews;

public class OdysseyAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "OdysseyWidget";

    private MusicLibraryHelper.CoverBitmapGenerator mCoverGenerator;
    private RemoteViews mViews;
    private AppWidgetManager mAppWidgetManager;
    private int[] mAppWidgets = null;
    private Context mContext;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.v(TAG, "onUpdate");
        mContext = context;

        final int N = appWidgetIds.length;
        mAppWidgets = appWidgetIds;

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
            Intent playPauseIntent = new Intent(context, PlaybackService.class);
            playPauseIntent.putExtra("action", PlaybackService.ACTION_TOGGLEPAUSE);
            PendingIntent playPausePendingIntent = PendingIntent.getService(context, 0, playPauseIntent, 0);
            views.setOnClickPendingIntent(R.id.odysseyWidgetPlaypauseButton, playPausePendingIntent);

            // Previous song action
            Intent prevIntent = new Intent(context, PlaybackService.class);
            prevIntent.putExtra("action", PlaybackService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getService(context, 1, prevIntent, 0);
            views.setOnClickPendingIntent(R.id.odysseyWidgetPreviousButton, prevPendingIntent);

            // Next song action
            Intent nextIntent = new Intent(context, PlaybackService.class);
            nextIntent.putExtra("action", PlaybackService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getService(context, 2, nextIntent, 0);
            views.setOnClickPendingIntent(R.id.odysseyWidgetNextButton, nextPendingIntent);

            // Quit action
            Intent stopIntent = new Intent(context, PlaybackService.class);
            stopIntent.putExtra("action", PlaybackService.ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getService(context, 3, stopIntent, 0);
            views.setOnClickPendingIntent(R.id.odysseyWidgetStopButton, stopPendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app
            // widget
            mAppWidgetManager = appWidgetManager;
            appWidgetManager.updateAppWidget(appWidgetId, views);
            mViews = views;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);
        Log.v(TAG, "Onreceive");
        mContext = context;
        // get remoteviews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.odyssey_appwidget);

        if (intent.getAction().equals(PlaybackService.MESSAGE_NEWTRACKINFORMATION)) {

            intent.setExtrasClassLoader(context.getClassLoader());

            ArrayList<Parcelable> trackItemList = intent.getParcelableArrayListExtra(PlaybackService.INTENT_TRACKITEMNAME);

            if (trackItemList.size() == 1) {
                TrackItem item = (TrackItem) trackItemList.get(0);

                views.setTextViewText(R.id.odysseyWidgetTextViewTrack, item.getTrackTitle());
                views.setTextViewText(R.id.odysseyWidgetTextViewArtist, item.getTrackArtist());
                //
                // // String where =
                // // android.provider.MediaStore.Audio.Albums.ALBUM_KEY + "=?";
                // //
                // // String whereVal[] = { item.getTrackAlbumKey() };
                // //
                // // Cursor cursor =
                // //
                // context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                // // new String[] { MediaStore.Audio.Albums.ALBUM_ART }, where,
                // // whereVal, "");
                // //
                // // String coverPath = null;
                // // if (cursor.moveToFirst()) {
                // // coverPath =
                // //
                // cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                // // }
                // //
                // // if (coverPath != null) {
                // // BitmapDrawable cover = (BitmapDrawable)
                // // BitmapDrawable.createFromPath(coverPath);
                // //
                // // views.setImageViewBitmap(R.id.odysseyWidgetImageView,
                // // cover.getBitmap());
                // // } else {
                // // views.setImageViewResource(R.id.odysseyWidgetImageView,
                // // R.drawable.coverplaceholder);
                // // }
                views.setImageViewResource(R.id.odysseyWidgetImageView, R.drawable.ic_big_notification);
                mCoverGenerator = new CoverBitmapGenerator(context, new CoverReceiver(views));
                mCoverGenerator.getImage(item);
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
        Intent playPauseIntent = new Intent(context, PlaybackService.class);
        playPauseIntent.putExtra("action", PlaybackService.ACTION_TOGGLEPAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(context, 0, playPauseIntent, 0);
        views.setOnClickPendingIntent(R.id.odysseyWidgetPlaypauseButton, playPausePendingIntent);

        // Previous song action
        Intent prevIntent = new Intent(context, PlaybackService.class);
        prevIntent.putExtra("action", PlaybackService.ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getService(context, 1, prevIntent, 0);
        views.setOnClickPendingIntent(R.id.odysseyWidgetPreviousButton, prevPendingIntent);

        // Next song action
        Intent nextIntent = new Intent(context, PlaybackService.class);
        nextIntent.putExtra("action", PlaybackService.ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(context, 2, nextIntent, 0);
        views.setOnClickPendingIntent(R.id.odysseyWidgetNextButton, nextPendingIntent);

        // Quit action
        Intent stopIntent = new Intent(context, PlaybackService.class);
        stopIntent.putExtra("action", PlaybackService.ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(context, 3, stopIntent, 0);
        views.setOnClickPendingIntent(R.id.odysseyWidgetStopButton, stopPendingIntent);
        mViews = views;
        mAppWidgetManager = AppWidgetManager.getInstance(context);
        mAppWidgetManager.updateAppWidget(new ComponentName(context, OdysseyAppWidgetProvider.class), views);
    }

    private class CoverReceiver implements MusicLibraryHelper.CoverBitmapListener {

        public CoverReceiver(RemoteViews views) {
        }

        @Override
        public void receiveBitmap(BitmapDrawable bm) {

            if (mViews != null && bm != null) {
                mViews.setImageViewBitmap(R.id.odysseyWidgetImageView, bm.getBitmap());

                mAppWidgetManager.updateAppWidget(new ComponentName(mContext, OdysseyAppWidgetProvider.class), mViews);

            }
        }
    }

}
