package me.piclane.util;

import java.util.Map;

/**
 * Interface for message consuming.
 */
public interface MessageConsumer {
    /**
     * Consuming messages
     *
     * @param message Message
     * @return true if the next message is requested, otherwise false
     */
    boolean consume(Map<String, Object> message);
}
