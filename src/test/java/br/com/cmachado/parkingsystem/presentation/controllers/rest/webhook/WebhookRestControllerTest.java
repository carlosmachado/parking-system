package br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook;

import br.com.cmachado.parkingsystem.application.parkingsession.ParkingSessionService;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.GarageFullException;
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
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookRestController.class)
class WebhookRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkingSessionService webhookService;

    @Test
    void eventReturns200AndDelegates() throws Exception {
        String body = """
                {"license_plate":"ZUL0001","entry_time":"2025-01-01T12:00:00.000Z","event_type":"ENTRY"}
                """;

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(webhookService).handle(any());
    }

    @Test
    void serviceThrowsBadRequestReturns400() throws Exception {
        doThrow(new BadRequestException("event_type is required")).when(webhookService).handle(any());

        String body = """
                {"license_plate":"ZUL0001"}
                """;

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void entryWhenGarageFullReturns409WithErrorCode() throws Exception {
        doThrow(new GarageFullException(LicensePlate.of("ZUL0001"))).when(webhookService).handle(any());

        String body = """
                {"license_plate":"ZUL0001","entry_time":"2025-01-01T12:00:00.000Z","event_type":"ENTRY"}
                """;

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EST-001"))
                .andExpect(jsonPath("$.message", startsWith("Garage is full")));
    }
}
