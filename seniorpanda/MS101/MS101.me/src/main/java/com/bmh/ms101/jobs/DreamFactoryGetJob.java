package com.bmh.ms101.jobs;

import android.util.Log;

import com.bmh.ms101.Backend;
import com.bmh.ms101.MS101;
import com.bmh.ms101.events.GetDataDFEvent;
import com.bmh.ms101.ex.DFCredentialsInvalidException;
import com.bmh.ms101.models.BaseRecordModel;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.ArrayList;
import java.util.Date;

import de.greenrobot.event.EventBus;

/**
 * Job that handles getting data from the Dreamfactory server.
 */
public class DreamFactoryGetJob extends Job {
    public static final int PRIORITY = 50;

    private Date from, to;

    /**
     * Create a new job to get data from DreamFactory.
     * @param from Earliest date to get data from
     * @param to Latest date to get data from
     */
    public DreamFactoryGetJob(Date from, Date to) {
        super(new Params(PRIORITY).requireNetwork().persist());
        this.from = from;
        this.to = to;
    }

    @Override
    public void onAdded() {
        // Nothing for now
    }

    @Override
    public void onRun() throws Throwable {
        Backend backend = new Backend(MS101.getInstance());
        ArrayList<BaseRecordModel> userRecords = backend.getFromDF(from, to);
        if (userRecords != null) {
            EventBus.getDefault().post(new GetDataDFEvent(true, userRecords));
        } else {
            throw new DFCredentialsInvalidException();
        }
    }

    @Override
    protected void onCancel() {
        // Nothing
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        if (throwable instanceof Exception) {
            Exception e = (Exception) throwable;
            Log.w("DreamFactory Job", "Failed to get data", e);
            EventBus.getDefault().post(new GetDataDFEvent(false, e));
        } else {
            // Not sure how it wouldn't be an Exception object, but I digress...
            throwable.printStackTrace();
        }
        return false;
    }
}
