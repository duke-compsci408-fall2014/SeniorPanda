package com.example.main.bamboohealth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.app.NotificationManager;
import android.app.Notification;
import android.content.Context;
import android.app.PendingIntent;


public class MainMenu extends Activity {
    NotificationManager NM;
    int id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void createViewHealth(View view){
        Intent intent = new Intent(this, SummaryListActivity.class);
        startActivity(intent);
    }

    public void createPhotoFlip(View view) {
        Intent intent = new Intent(this, PhotoFlipActivity.class);
        startActivity(intent);
    }

    public void notify(View vobj){
        id++;
        NM=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notify=new Notification(android.R.drawable.
                stat_notify_more, "Test Notification: " + id,System.currentTimeMillis());
        PendingIntent pending=PendingIntent.getActivity(
                getApplicationContext(),0, new Intent(),0);
        notify.setLatestEventInfo(getApplicationContext(),"Notify Subject: " + id, "Notify Body: " + id,pending);
        NM.notify(id, notify);

    }

}
