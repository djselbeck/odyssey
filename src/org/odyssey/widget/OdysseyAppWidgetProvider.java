package org.odyssey.widget;

import org.odyssey.MainActivity;
import org.odyssey.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class OdysseyAppWidgetProvider extends AppWidgetProvider {
	
	public static final String ACTION_PLAY = "org.odyssey.play";
	public static final String ACTION_PAUSE = "org.odyssey.pause";
	public static final String ACTION_NEXT = "org.odyssey.next";
	public static final String ACTION_PREVIOUS = "org.odyssey.previous";
	public static final String ACTION_STOP = "org.odyssey.stop";	

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
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
            Intent playIntent = new Intent(ACTION_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getBroadcast(context, 42, playIntent, PendingIntent.FLAG_UPDATE_CURRENT); 
            views.setOnClickPendingIntent(R.id.odysseyWidgetPlaypauseButton, playPendingIntent);
            
//			Intent pauseIntent = new Intent(ACTION_PAUSE);
//			PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, 42, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);            
//			views.setOnClickPendingIntent(R.id.odysseyWidgetPlaypauseButton, pausePendingIntent);
			
            // Previous song action
    		Intent prevIntent = new Intent(ACTION_PREVIOUS);
    		PendingIntent prevPendingIntent = PendingIntent.getBroadcast(context, 42, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.odysseyWidgetPreviousButton, prevPendingIntent);
            
    		// Next song action
    		Intent nextIntent = new Intent(ACTION_NEXT);
    		PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 42, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);            
    		views.setOnClickPendingIntent(R.id.odysseyWidgetNextButton, nextPendingIntent);
    		
    		// Quit action
    		Intent stopIntent = new Intent(ACTION_STOP);
    		PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 42, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);    		
            views.setOnClickPendingIntent(R.id.odysseyWidgetStopButton, stopPendingIntent);
            
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

	
	
}
