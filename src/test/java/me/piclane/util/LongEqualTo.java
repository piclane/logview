package me.piclane.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * If the actual object is of type Number, is it the equal to the expected long value?
 */
public class LongEqualTo extends BaseMatcher<Object> {
    /** Expected long value */
    private final long value;

    /**
     * Constructor
     *
     * @param value Expected long value
     */
    public LongEqualTo(long value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(Object item) {
        if(item instanceof Number) {
            long other = ((Number)item).longValue();
            return value == other;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void describeTo(Description description) {
        description.appendValue(value);
    }
}
