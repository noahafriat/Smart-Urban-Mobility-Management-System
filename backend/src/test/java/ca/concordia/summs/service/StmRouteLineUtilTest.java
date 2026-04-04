package ca.concordia.summs.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StmRouteLineUtilTest {

    @Test
    void normalizeBusLine_trimsAndRemovesWhitespace() {
        assertEquals("165", StmRouteLineUtil.normalizeBusLine("  165 "));
        assertEquals("10", StmRouteLineUtil.normalizeBusLine("1 0"));
    }

    @Test
    void normalizeBusLine_nullReturnsEmpty() {
        assertEquals("", StmRouteLineUtil.normalizeBusLine(null));
    }

    @Test
    void routeMatches_exactRouteId() {
        assertTrue(StmRouteLineUtil.routeMatches("165", "165"));
    }

    @Test
    void routeMatches_suffixPattern() {
        assertTrue(StmRouteLineUtil.routeMatches("BUS_165", "165"));
        assertTrue(StmRouteLineUtil.routeMatches("STM_ROUTE_165_0", "165"));
    }

    @Test
    void routeMatches_lastSegmentAfterUnderscore() {
        assertTrue(StmRouteLineUtil.routeMatches("something_80", "80"));
    }

    @Test
    void routeMatches_blankRouteId_false() {
        assertFalse(StmRouteLineUtil.routeMatches("", "165"));
        assertFalse(StmRouteLineUtil.routeMatches(null, "165"));
    }

    @Test
    void routeMatches_wrongLine_false() {
        assertFalse(StmRouteLineUtil.routeMatches("166", "165"));
    }
}
