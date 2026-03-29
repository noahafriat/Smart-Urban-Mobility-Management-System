package ca.concordia.summs.service;

/**
 * Matches STM GTFS / realtime {@code route_id} values to a citizen-entered bus line number (digits).
 */
public final class StmRouteLineUtil {

    private StmRouteLineUtil() {}

    public static String normalizeBusLine(String lineRaw) {
        if (lineRaw == null) {
            return "";
        }
        return lineRaw.trim().replaceAll("\\s+", "");
    }

    public static boolean routeMatches(String routeId, String lineDigits) {
        if (routeId == null || routeId.isBlank()) {
            return false;
        }
        String r = routeId.trim();
        if (r.equals(lineDigits)) {
            return true;
        }
        String tagged = "_" + lineDigits;
        if (r.endsWith(tagged)) {
            return true;
        }
        if (r.contains(tagged + "_")) {
            return true;
        }
        int last = r.lastIndexOf('_');
        return last >= 0 && r.substring(last + 1).equals(lineDigits);
    }
}
