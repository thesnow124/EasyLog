package com.github.easylog.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholderResolverTest {

    @Test
    void resolvesSequentialPlaceholders() {
        PlaceholderResolver resolver = PlaceholderResolver.getDefaultResolver();
        String resolved = resolver.resolve("a=${},b=${}", "1", "2");
        assertEquals("a=1,b=2", resolved);
    }

    @Test
    void stopsWhenValuesExhausted() {
        PlaceholderResolver resolver = PlaceholderResolver.getDefaultResolver();
        String resolved = resolver.resolve("a=${},b=${},c=${}", "1", "2");
        assertEquals("a=1,b=2,c=${}", resolved);
    }

    @Test
    void resolvesIndexedPlaceholders() {
        PlaceholderResolver resolver = PlaceholderResolver.getDefaultResolver();
        String resolved = resolver.resolve("a=${1},b=${0}", "first", "second");
        assertEquals("a=second,b=first", resolved);
    }

    @Test
    void resolvesMixedSequentialAndIndexedPlaceholders() {
        PlaceholderResolver resolver = PlaceholderResolver.getDefaultResolver();
        String resolved = resolver.resolve("a=${},b=${1},c=${}", "x", "y", "z");
        assertEquals("a=x,b=y,c=y", resolved);
    }

    @Test
    void keepsIndexedPlaceholderWhenOutOfRange() {
        PlaceholderResolver resolver = PlaceholderResolver.getDefaultResolver();
        String resolved = resolver.resolve("a=${2},b=${}", "x");
        assertEquals("a=${2},b=x", resolved);
    }
}
