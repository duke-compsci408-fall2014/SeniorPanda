package com.bmh.ms101.events;

/**
 * Event fired when we finish trying to get data from DreamFactory.
 */
public class GetDataDFEvent {

    public final boolean wasSuccess;
    public final Object response;

    /**
     * Create a new event indicating that we're done trying to send medication data to DreamFactory
     * @param wasSuccess True if we successfully sent the data
     * @param response ArrayList of BaseRecordModel or some Exception
     */
    public GetDataDFEvent(boolean wasSuccess, Object response) {
        this.wasSuccess = wasSuccess;
        this.response = response;
    }
}
