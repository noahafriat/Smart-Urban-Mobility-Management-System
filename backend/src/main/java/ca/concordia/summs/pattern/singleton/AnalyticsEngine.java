package ca.concordia.summs.pattern.singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton Pattern implementation.
 * Used for centralizing analytics events across the entire application securely.
 */
public class AnalyticsEngine {
    private static AnalyticsEngine instance;
    private List<String> eventLogs = new ArrayList<>();

    private AnalyticsEngine() {}

    public static synchronized AnalyticsEngine getInstance() {
        if (instance == null) {
            instance = new AnalyticsEngine();
        }
        return instance;
    }

    public void logEvent(String eventType, String detail) {
        eventLogs.add(System.currentTimeMillis() + " | " + eventType + ": " + detail);
        System.out.println("ANALYTICS: " + eventType + " - " + detail);
    }
    
    public List<String> getEventLogs() { return eventLogs; }
}
