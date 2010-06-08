/**
 * 
 */
package com.google.code.morphia.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ReflectionUtilsTest extends TestCase
{

    /**
     * Test method for
     * {@link com.google.code.morphia.utils.ReflectionUtils#implementsInterface(java.lang.Class, java.lang.Class)}
     * .
     */
    public void testImplementsInterface()
    {
        assertTrue(ReflectionUtils.implementsInterface(ArrayList.class, List.class));
        assertTrue(ReflectionUtils.implementsInterface(ArrayList.class, Collection.class));
        assertFalse(ReflectionUtils.implementsInterface(Set.class, List.class));
    }

}
