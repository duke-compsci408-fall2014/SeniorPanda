package com.bmh.ms101.jobs;

import android.util.Log;

import com.bmh.ms101.Backend;
import com.bmh.ms101.MS101;
import com.bmh.ms101.events.GetDataDFEvent;
import com.bmh.ms101.events.GetMedsDFEvent;
import com.bmh.ms101.events.GetSubscribeDFEvent;
import com.bmh.ms101.ex.DFCredentialsInvalidException;
import com.bmh.ms101.models.BaseDataModel;
import com.bmh.ms101.models.BaseRecordModel;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;

import com.bmh.ms101.User;

/**
 * Job that handles getting data from the Dreamfactory server.
 */
public class DreamFactoryGetJob extends Job {
    public static final int PRIORITY = 50;

    private Date from, to;
    private int dataType;
    private String data;
    private long timeSent;

    /**
     * Create a new job to get data from DreamFactory.
     * @param from Earliest date to get data from
     * @param to Latest date to get data from
     */
    public DreamFactoryGetJob(Date from, Date to) {
        super(new Params(PRIORITY).requireNetwork().persist());
        this.from = from;
        this.to = to;
        this.dataType = User.OLD_USER_RECORD_DATA_TYPE;
    }

    public DreamFactoryGetJob(int dataType) {
        super(new Params(PRIORITY).requireNetwork().persist());
        this.dataType = dataType;
    }

    @Override
    public void onAdded() {
        // Nothing for now
    }

    @Override
    public void onRun() throws Throwable {
        Backend backend = new Backend(MS101.getInstance());
        if (dataType == User.OLD_USER_RECORD_DATA_TYPE) {
            ArrayList<BaseRecordModel> userRecords = backend.getFromDF(from, to);
            if (userRecords != null) {
                EventBus.getDefault().post(new GetDataDFEvent(true, userRecords));
                // indicating events are necessary

            } else {
                throw new DFCredentialsInvalidException();
            }
        } else if (dataType == User.MEDICATION_DATA_TYPE) {
            List<BaseDataModel> data = backend.getFromDF(dataType);
            if (data != null) {
                EventBus.getDefault().post(new GetMedsDFEvent(true, data));
            } else {
                throw new DFCredentialsInvalidException();
            }
        } else if (dataType == User.SUBSCRIBE_DATA_TYPE) {
            List<BaseDataModel> data = backend.getFromDF(dataType);
            if (data != null) {
                EventBus.getDefault().post(new GetSubscribeDFEvent(true, data));
            } else {
                throw new DFCredentialsInvalidException();
            }
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
