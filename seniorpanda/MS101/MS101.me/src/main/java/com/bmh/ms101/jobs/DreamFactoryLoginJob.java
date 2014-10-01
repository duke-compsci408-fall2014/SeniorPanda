package com.bmh.ms101.jobs;

import com.bmh.ms101.Backend;
import com.bmh.ms101.MS101;
import com.bmh.ms101.User;
import com.bmh.ms101.events.DFLoginResponseEvent;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import de.greenrobot.event.EventBus;

/**
 * Job that handles logging in to DreamFactory. (Not quick login)
 */
public class DreamFactoryLoginJob extends Job {
    public static final int PRIORITY = 75;

    private boolean isRefreshing;
    private boolean isFromAlarm;

    /**
     * Create a new job to log in to DreamFactory.
     */
    public DreamFactoryLoginJob() {
        this(false);
    }

    /**
     * Create a new job to log in to DreamFactory.
     * @param isRefreshing If we're ignoring the existing token in order to get a new one.
     */
    public DreamFactoryLoginJob(boolean isRefreshing) {
        this(isRefreshing, false);
    }

    /**
     * Create a new job to log in to DreamFactory.
     * @param isRefreshing If we're ignoring the existing token in order to get a new one.
     * @param isFromAlarm If we're just refreshing the session ID due to an alarm.
     */
    public DreamFactoryLoginJob(boolean isRefreshing, boolean isFromAlarm) {
        super(new Params(PRIORITY).requireNetwork().persist());
        this.isRefreshing = isRefreshing;
        this.isFromAlarm = isFromAlarm;
    }

    @Override
    public void onAdded() {
        // Do nothing
    }

    @Override
    public void onRun() throws Throwable {
        Backend backend = new Backend(MS101.getInstance());
        backend.tryDFLogin(isRefreshing);
        // Post an event indicating success unless we were started by an alarm.
        if (!isFromAlarm) EventBus.getDefault().post(new DFLoginResponseEvent("Success"));
    }

    @Override
    protected void onCancel() {
        // Do nothing
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        if (throwable instanceof Exception) {
            Exception e = (Exception) throwable;
            if (!isFromAlarm) {
                // We started this job in a normal way, just post an event with the exception.
                EventBus.getDefault().post(new DFLoginResponseEvent(e));
            } else {
                // We were just trying to refresh our session ID, but we failed for some reason, so
                // record a blank session ID so that user will be asked to login later.
                new User(MS101.getInstance()).recordDFSessionId("");
            }
        } else {
            throwable.printStackTrace();
        }
        return false;
    }
}
