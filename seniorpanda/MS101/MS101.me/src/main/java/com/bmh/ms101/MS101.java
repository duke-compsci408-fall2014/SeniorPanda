package com.bmh.ms101;

import android.app.Application;

import com.path.android.jobqueue.JobManager;

/**
 * Custom Application class for MS101.me
 */
public class MS101 extends Application {
    private static MS101 mAppInstance;
    private JobManager mJobManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppInstance = this;
        mJobManager = new JobManager(this);
    }

    /**
     * Gets an instance of our application class. Also functions as the application Context.
     * @return Instance of MS101Application
     */
    public static MS101 getInstance() {
        return mAppInstance;
    }

    /**
     * Gets the application's JobManager
     * @return JobManager tied to this application
     */
    public JobManager getJobManager() {
        return mJobManager;
    }

}
