package org.lts.tabnamedimmer;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LineOfSightCacheTest {
    @Test
    void cacheStorageSupportsConcurrentRenderAccess() throws ReflectiveOperationException {
        Field values = LineOfSightCache.class.getDeclaredField("values");
        values.setAccessible(true);

        assertInstanceOf(ConcurrentMap.class, values.get(LineOfSightCache.INSTANCE));
    }
}
