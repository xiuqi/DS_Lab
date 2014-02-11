package ds.lab.core.event;

import ds.lab.entity.Message;

public interface IEventListener {

    public enum Status {
        SUCCESS, FAILURE
    }

    /**
     * Deliver callback with Status
     * 
     * @param message the message will be delivered
     * @param status
     */
    public void onDeliver(Message message, Status status);

    /**
     * Pending callback with Status
     * 
     * @param message
     * @param status
     */
    public void onPending(Message message, Status status);

    /**
     * Receive callback with Status
     * 
     * @param message
     * @param status
     */
    public void onReceive(Message message, Status status);

}
