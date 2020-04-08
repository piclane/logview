package me.piclane.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * If the actual object is of type Number, is it the equal to the expected int value?
 */
public class IntEqualTo extends BaseMatcher<Object> {
    /** Expected int value */
    private final int value;

    /**
     * Constructor
     *
     * @param value Expected int value
     */
    public IntEqualTo(int value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(Object item) {
        if(item instanceof Number) {
            int other = ((Number)item).intValue();
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
