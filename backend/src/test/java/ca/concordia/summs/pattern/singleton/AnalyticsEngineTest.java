package ca.concordia.summs.pattern.singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsEngineTest {

    @AfterEach
    void resetSingleton() throws Exception {
        Field f = AnalyticsEngine.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    void getInstance_returnsSameInstance() {
        AnalyticsEngine a = AnalyticsEngine.getInstance();
        assertSame(a, AnalyticsEngine.getInstance());
    }

    @Test
    void logEvent_recordsEntry() {
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        int before = engine.getEventLogs().size();
        engine.logEvent("TEST_EVENT", "detail");
        assertTrue(engine.getEventLogs().size() > before);
    }
}
