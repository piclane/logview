package me.piclane.util;

import com.google.gson.Gson;

import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * StringWriter to consume messages sent out asynchronously.
 */
public class MessageConsumableWriter extends StringWriter {
    /** LinkedBlockingQueue to store strings sent in different threads */
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    /** gson */
    private final Gson gson = new Gson();

    /** Last received messages */
    private Deque<Map<String, Object>> messages = null;

    /**
     * Consume the messages one by one.
     *
     * @param consumer {@link MessageConsumer}
     * @throws InterruptedException If interrupted while taking a message from the queue
     */
    public void consume(MessageConsumer consumer) throws InterruptedException {
        do {
            if (messages == null || messages.isEmpty()) {
                String json = queue.take();
                assertThat(json, is(allOf(startsWith("["), endsWith("]"))));
                this.messages = gson.fromJson(json, Deque_Map_Type);
            }
        } while(consumer.consume(messages.pollFirst()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        StringBuffer buf = super.getBuffer();
//        System.out.println(buf);
        queue.add(buf.toString());
        buf.setLength(0);
    }

    /**
     * Represent Deque&lt;Map&lt;String, Object>> type
     */
    private static final Type Deque_Map_Type = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] { Map_String_Object_Type };
        }

        @Override
        public Type getRawType() {
            return Deque.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    };

    /**
     * Represent Map&lt;String, Object> type
     */
    private static final Type Map_String_Object_Type = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] { String.class, Object.class };
        }

        @Override
        public Type getRawType() {
            return Map.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    };
}
