package com.bmh.ms101.events;

public class GetLogsDFEvent {
    public final boolean wasSuccess;
    public final Object response;

    /**
     * Create a new event indicating that we're done trying to send medication data to DreamFactory
     * @param wasSuccess True if we successfully sent the data
     * @param response ArrayList of BaseRecordModel or some Exception
     */
    public GetLogsDFEvent(boolean wasSuccess, Object response) {
        this.wasSuccess = wasSuccess;
        this.response = response;
    }
}
