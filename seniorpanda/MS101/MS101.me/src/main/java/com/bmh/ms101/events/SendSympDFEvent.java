package com.bmh.ms101.events;

/**
 * Event fired when we finish trying to send symptoms to DreamFactory.
 */
public class SendSympDFEvent {

    public final boolean wasSuccess;
    public final Object response;

    /**
     * Create a new event indicating that we're done trying to send symptom data to DreamFactory.
     * @param wasSuccess True if we successfully sent the data
     * @param response Response JSON string
     */
    public SendSympDFEvent(boolean wasSuccess, Object response) {
        this.wasSuccess = wasSuccess;
        this.response = response;
    }
}
