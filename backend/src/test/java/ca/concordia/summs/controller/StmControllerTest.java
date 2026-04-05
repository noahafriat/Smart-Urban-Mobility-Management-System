package ca.concordia.summs.controller;

import ca.concordia.summs.service.StmGatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StmController.class)
class StmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StmGatewayService stmGatewayService;

    @Test
    void busStops_returnsPayloadFromService() throws Exception {
        when(stmGatewayService.getBusStopsForLine(eq("165"), eq(false)))
                .thenReturn(Map.of(
                        "configured", true,
                        "line", "165",
                        "stops", List.of(Map.of("stopId", "s1", "label", "Stop 1"))));

        mockMvc.perform(get("/api/stm/bus-stops").param("line", "165"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.line").value("165"))
                .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    void busDepartures_returnsPayloadFromService() throws Exception {
        when(stmGatewayService.getBusDepartures(eq("80"), eq("12345"), eq(false)))
                .thenReturn(Map.of("configured", true, "line", "80", "count", 0));

        mockMvc.perform(get("/api/stm/bus-departures")
                        .param("line", "80")
                        .param("stopCode", "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.line").value("80"));
    }

    @Test
    void debugTripSample_returnsPayload() throws Exception {
        when(stmGatewayService.debugTripSampleForLine(eq("165")))
                .thenReturn(Map.of("configured", true, "line", "165"));

        mockMvc.perform(get("/api/stm/debug/trip-sample").param("line", "165"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.line").value("165"));
    }
}
