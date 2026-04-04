package ca.concordia.summs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StmGtfsStopsServiceTest {

    private StmGtfsStopsService service;

    @BeforeEach
    void setUp() {
        service = new StmGtfsStopsService(new ClassPathResource("gtfs-test/stops_sample.csv"));
        service.loadStops();
    }

    @Test
    void lookup_findsStopByStopId() {
        Optional<StmGtfsStopsService.StopMeta> m = service.lookup("STM_99901");
        assertTrue(m.isPresent());
        assertEquals("Sample Boulevard / Test", m.get().stopName());
        assertEquals("99901", m.get().stopCode());
    }

    @Test
    void lookup_findsStopByFiveDigitStopCode() {
        Optional<StmGtfsStopsService.StopMeta> m = service.lookup("99901");
        assertTrue(m.isPresent());
        assertEquals("Sample Boulevard / Test", m.get().stopName());
    }

    @Test
    void lookup_extractsLastFiveDigitGroupFromCompositeId() {
        Optional<StmGtfsStopsService.StopMeta> m = service.lookup("prefix_99902_trailing");
        assertTrue(m.isPresent());
        assertEquals("Other Corner Stop", m.get().stopName());
    }

    @Test
    void lookup_blankReturnsEmpty() {
        assertTrue(service.lookup("").isEmpty());
        assertTrue(service.lookup("   ").isEmpty());
        assertTrue(service.lookup(null).isEmpty());
    }

    @Test
    void lookup_unknownStopReturnsEmpty() {
        assertTrue(service.lookup("NO_SUCH_STOP").isEmpty());
    }
}
