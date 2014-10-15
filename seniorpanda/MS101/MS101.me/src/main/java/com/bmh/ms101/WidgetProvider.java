package com.bmh.ms101;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/**
 * Manages the widgets
 */
public class WidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        User mUser = new User(context);
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            // Only try to update a widget if it's actually set up
            if (!mUser.getMedKey(appWidgetId).equals("null")) {
                // Represent the widget and then pass the views to be refreshed (Any alarms will also be refreshed if need be)
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_md);
                SetupMDWidgetActivity.refreshRemoteViewsAndAlarms(context, appWidgetId, views);
                // Tell the AppWidgetManager to perform an update on the current app widget
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        User mUser = new User(context);
        for (int widgetId : appWidgetIds) {
            // Get the ID of the med that the deleted widget was bound to
            String medKey = mUser.getMedKey(widgetId).replace("null", "");
            // We need to stop keeping track of this widget/med pair
            mUser.removeMedDoseWidgetId(medKey, widgetId);
            // And finally, cancel any special alarms that might be running for this widget/med pair
            MS101Receiver.cancelSpecialAlarms(context, medKey);
        }
    }


}
