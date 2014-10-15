package com.bmh.ms101.events;

/**
 * Fired when we get a response from DreamFactory after trying to log in.
 */
public class DFLoginResponseEvent {

    public final Object response;

    /**
     * Create a new event indicating we got a response from DreamFactory after we tried to log in.
     * @param response The response we got from DreamFactory
     */
    public DFLoginResponseEvent(Object response) {
        this.response = response;
    }

}
