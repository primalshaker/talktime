package com.twd.talktime;

import java.util.Calendar;
import java.util.GregorianCalendar;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.provider.CallLog;

import android.widget.RemoteViews;

public class TalkTimeWidget extends AppWidgetProvider {

//	private static final String ACTION_CLICK = "ACTION_CLICK";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// Get all ids
		ComponentName thisWidget = new ComponentName(context, TalkTimeWidget.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		for (int widgetId : allWidgetIds) {

			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

			int m_billDay = Integer.parseInt(sharedPrefs.getString("pref_billDay", "18"));
			int m_telcoPlanMin = Integer.parseInt(sharedPrefs.getString("pref_planMinutes", "400"));
			int totalDuration = getDuration(context, MainActivity.getBillingCycleDate(m_billDay), null);
			int talktimeLeft = (m_telcoPlanMin - (totalDuration / 60));
			double ttuPercent = totalDuration/(60.0*m_telcoPlanMin);
//			Log.w("", "totalDuration:"+ totalDuration+ " ttuPercent: " + ttuPercent);
			String str = "Talktime exceeded: ";
			if (talktimeLeft < 0) {
				str = " " + (0 - talktimeLeft) + "min over";
			} else {
				str = " " + talktimeLeft + "min left";
			}
			// Set the output of duration and stuff here
			remoteViews.setTextViewText(R.id.update1, str);

			Calendar calendar = MainActivity.getBillingCycleDate(m_billDay);
			long dayselapsed = MainActivity.daysBetween((Calendar)calendar.clone(), GregorianCalendar.getInstance());
			calendar.add(Calendar.MONTH, 1);
			long daysremaining = MainActivity.daysBetween(GregorianCalendar.getInstance(), calendar);
			double tuPercent = dayselapsed/ (1.0*dayselapsed + 1.0*daysremaining -1.0);
//			Log.w("", "dayselapsed:"+dayselapsed+" daysremaining:"+daysremaining+" tuPercent: " + tuPercent);
			str = "in " + daysremaining + " days";
			remoteViews.setTextViewText(R.id.update2, str);
			
			// compare talktime utilized vs time utilized
			if (ttuPercent>tuPercent) {
				remoteViews.setTextColor(R.id.update1, Color.RED);
			} else {
				remoteViews.setTextColor(R.id.update1, Color.GREEN);
			}
			// Register an onClickListener
			Intent intent = new Intent(context, TalkTimeWidget.class);
			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.update1, pendingIntent);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}

	}

	private int getDuration(Context context, Calendar fromDate, Calendar toDate) {

		String searchQuery = "TYPE=" + CallLog.Calls.OUTGOING_TYPE + " AND DURATION>0" + " AND DATE>= "
				+ fromDate.getTime().getTime();
		// select outgoing calls
		if (toDate != null) {
			searchQuery += " AND DATE< " + toDate.getTime().getTime();
		}
		Cursor managedCursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, searchQuery, null,
				null);
		int c_duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
		int totalDuration = 0;

//		Log.w("", "number of records: " + managedCursor.getCount());

		while (managedCursor.moveToNext()) {
			int callDuration = managedCursor.getInt(c_duration);
			totalDuration += callDuration; // only for current billing cycle
		}
		managedCursor.close();

		return totalDuration;
	}


}
