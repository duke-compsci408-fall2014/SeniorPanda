package com.bmh.ms101.jobs;

import android.util.Log;

import com.bmh.ms101.Backend;
import com.bmh.ms101.MS101;
import com.bmh.ms101.User;
import com.bmh.ms101.events.SendMedsDFEvent;
import com.bmh.ms101.events.SendStressDFEvent;
import com.bmh.ms101.events.SendSubscribeDFEvent;
import com.bmh.ms101.events.SendSympDFEvent;
import com.bmh.ms101.events.SendTakenDFEvent;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import de.greenrobot.event.EventBus;

/**
 * Job that handles sending data to the Dreamfactory server.
 */
public class DreamFactorySendJob extends Job {
    public static final int PRIORITY = 50;

    private int dataType;
    private String data;
    private long timeSent;

    /**
     * Create a new job to send data to DreamFactory.
     * @param dataType Type of data to send
     * @param data Data to send
     * @param timeSent Time data was reported by user
     */
    public DreamFactorySendJob(int dataType, String data, long timeSent) {
        super(new Params(PRIORITY).requireNetwork().persist());
        this.dataType = dataType;
        this.data = data;
        this.timeSent = timeSent;
    }

    @Override
    public void onAdded() {
        // Nothing for now
    }

    @Override
    public void onRun() throws Throwable {
        Backend backend = new Backend(MS101.getInstance());
        backend.sendToDF(dataType, data, String.valueOf(timeSent));
        switch (dataType) {
            case User.MED:
                EventBus.getDefault().post(new SendMedsDFEvent(true, ""));
                break;
            case User.SYMP:
                EventBus.getDefault().post(new SendSympDFEvent(true, ""));
                break;
            case User.STRESS:
                EventBus.getDefault().post(new SendStressDFEvent(true, ""));
                break;
            case User.TAKEN_DATA_TYPE:
                EventBus.getDefault().post(new SendTakenDFEvent(true, ""));
                break;
            case User.SUBSCRIBE_DATA_TYPE:
                EventBus.getDefault().post(new SendSubscribeDFEvent(true, ""));
                break;
            default:
                break;
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
            Log.w("DreamFactory Job", "Failed to send data", e);
            switch (dataType) {
                case User.MED:
                    EventBus.getDefault().post(new SendMedsDFEvent(false, e));
                    break;
                case User.SYMP:
                    EventBus.getDefault().post(new SendSympDFEvent(false, e));
                    break;
                case User.STRESS:
                    EventBus.getDefault().post(new SendStressDFEvent(false, e));
                    break;
                case User.TAKEN_DATA_TYPE:
                    EventBus.getDefault().post(new SendTakenDFEvent(false, e));
                    break;
                case User.SUBSCRIBE_DATA_TYPE:
                    EventBus.getDefault().post(new SendSubscribeDFEvent(true, ""));
                    break;
                default:
                    break;
            }
        } else {
            // Not sure how it wouldn't be an Exception object, but I digress...
            throwable.printStackTrace();
        }
        return false;
    }
}
