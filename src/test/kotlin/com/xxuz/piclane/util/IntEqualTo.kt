package com.xxuz.piclane.util

import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * If the actual object is of type Number, is it the equal to the expected int value?
 */
class IntEqualTo (
        /** Expected int value  */
        private val value: Int
) : BaseMatcher<Any?>() {
    /**
     * {@inheritDoc}
     */
    override fun matches(item: Any): Boolean {
        if (item is Number) {
            val other = item.toInt()
            return value == other
        }
        return false
    }

    /**
     * {@inheritDoc}
     */
    override fun describeTo(description: Description) {
        description.appendValue(value)
    }
}
