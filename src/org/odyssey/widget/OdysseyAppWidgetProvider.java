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
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.widget.RemoteViews;
import android.widget.Toast;

public class OdysseyAppWidgetProvider extends AppWidgetProvider {
	
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // get remoteviews
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.odyssey_appwidget);            
            
            // Main action
            Intent mainIntent = new Intent(context, MainActivity.class);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);
            views.setOnClickPendingIntent(R.id.odysseyWidgetImageView, mainPendingIntent);
            
            // Play/Pause action
            Intent playIntent = new Intent(PlaybackService.ACTION_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getBroadcast(context, 42, playIntent, PendingIntent.FLAG_UPDATE_CURRENT); 
            views.setOnClickPendingIntent(R.id.odysseyWidgetPlaypauseButton, playPendingIntent);
            
//			Intent pauseIntent = new Intent(PlaybackService.ACTION_PAUSE);
//			PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, 42, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);            
//			views.setOnClickPendingIntent(R.id.odysseyWidgetPlaypauseButton, pausePendingIntent);
			
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
            
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

	@Override
	public void onReceive(Context context, Intent intent) {
		
		super.onReceive(context, intent);		
		
		if(intent.getAction().equals(PlaybackService.MESSAGE_NEWTRACKINFORMATION)) {
			
			intent.setExtrasClassLoader(context.getClassLoader());
			
            // get remoteviews
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.odyssey_appwidget);  			
			
			ArrayList<Parcelable> arrayList = intent.getParcelableArrayListExtra(PlaybackService.INTENT_TRACKITEMNAME);
			
			if(arrayList.size() == 2) {
				TrackItem item = (TrackItem) arrayList.get(0);
				NowPlayingInformation playInfo = (NowPlayingInformation) arrayList.get(1);
				
				String text = item.getTrackArtist() + " - " +item.getTrackTitle();
				
				views.setTextViewText(R.id.odysseyWidgetTextView, text);
			}
		}
		
	}
	
}
