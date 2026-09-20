package picocli.jsonspec;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ArgTypesTest {

    @Test
    public void namesReturnsEveryTypeToClassAccepts() {
        Set<String> names = ArgTypes.names();
        Set<Class<?>> resolved = new HashSet<Class<?>>();
        for (String name : names) {
            resolved.add(ArgTypes.toClass(name));
        }
        assertEquals(new HashSet<Class<?>>(Arrays.<Class<?>>asList(
                String.class, boolean.class, int.class, long.class, double.class, File.class)), resolved);
    }

    @Test
    public void namesIsUnmodifiable() {
        try {
            ArgTypes.names().add("Bogus");
            org.junit.Assert.fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void resolvesArrayTypeSuffix() {
        assertEquals(File[].class, ArgTypes.toClass("File[]"));
        assertEquals(String[].class, ArgTypes.toClass("String[]"));
        assertEquals(int[].class, ArgTypes.toClass("int[]"));
    }

    @Test
    public void rejectsArrayOfUnknownBaseType() {
        try {
            ArgTypes.toClass("Frobnicator[]");
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            org.junit.Assert.assertTrue(expected.getMessage().contains("Frobnicator"));
        }
    }

    @Test
    public void writesArrayTypeBackToItsSuffixedName() {
        assertEquals("File[]", ArgTypes.toName(File[].class));
        assertEquals("String[]", ArgTypes.toName(String[].class));
    }
}
