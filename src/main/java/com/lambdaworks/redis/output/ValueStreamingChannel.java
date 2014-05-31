package com.lambdaworks.redis.output;

/**
 * Streaming API for multiple Keys. You can implement this interface in order to receive a call to <code>onValue</code> on every
 * value.
 * 
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 16:19
 */
public interface ValueStreamingChannel<V> {
    /**
     * Called on every incoming value.
     * 
     * @param value
     */
    void onValue(V value);
}