package br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook;

import br.com.cmachado.parkingsystem.application.webhook.ParkingSessionService;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.infrastructure.http.GarageFullException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookRestController.class)
class WebhookRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkingSessionService webhookService;

    @Test
    void entryEventReturns200AndDelegates() throws Exception {
        String body = """
                {"license_plate":"ZUL0001","entry_time":"2025-01-01T12:00:00.000Z","event_type":"ENTRY"}
                """;

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(webhookService).processEntry(any());
    }

    @Test
    void parkedEventReturns200AndDelegates() throws Exception {
        String body = """
                {"license_plate":"ZUL0001","lat":-23.561684,"lng":-46.655981,"event_type":"PARKED"}
                """;

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(webhookService).processParked(any());
    }

    @Test
    void exitEventReturns200AndDelegates() throws Exception {
        String body = """
                {"license_plate":"ZUL0001","exit_time":"2025-01-01T12:30:00.000Z","event_type":"EXIT"}
                """;

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(webhookService).processExit(any());
    }

    @Test
    void unknownEventTypeReturns400() throws Exception {
        String body = """
                {"license_plate":"ZUL0001","event_type":"TELEPORT"}
                """;

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingEventTypeReturns400() throws Exception {
        String body = """
                {"license_plate":"ZUL0001"}
                """;

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void entryWhenGarageFullReturns409WithErrorCode() throws Exception {
        doThrow(new GarageFullException(LicensePlate.of("ZUL0001"))).when(webhookService).processEntry(any());

        String body = """
                {"license_plate":"ZUL0001","entry_time":"2025-01-01T12:00:00.000Z","event_type":"ENTRY"}
                """;

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EST-001"))
                .andExpect(jsonPath("$.message").value("Parking is full"));
    }
}
