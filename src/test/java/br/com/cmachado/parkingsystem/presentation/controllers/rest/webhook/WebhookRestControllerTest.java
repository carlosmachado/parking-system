package br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.mediator.WebhookEventMediator;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.GarageFullException;
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

    private static final String ENTRY_BODY = """
            {"license_plate":"ZUL0001","entry_time":"2025-01-01T12:00:00.000Z","event_type":"ENTRY"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookEventMediator webhookEventMediator;

    @Test
    void eventReturns200AndDelegates() throws Exception {
        // act
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(ENTRY_BODY))
                .andExpect(status().isOk());

        // assert
        verify(webhookEventMediator).handle(any());
    }

    @Test
    void serviceThrowsBadRequestReturns400() throws Exception {
        // arrange
        doThrow(new BadRequestException("event_type is required")).when(webhookEventMediator).handle(any());
        String body = """
                {"license_plate":"ZUL0001"}
                """;

        // act / assert
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEB-001"));
    }

    @Test
    void emptyBodyReturns400WithErrorCode() throws Exception {
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEB-001"));
    }

    @Test
    void entryWhenGarageFullReturns409WithErrorCode() throws Exception {
        // arrange
        doThrow(new GarageFullException(LicensePlate.of("ZUL0001"))).when(webhookEventMediator).handle(any());

        // act / assert
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(ENTRY_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EST-001"))
                .andExpect(jsonPath("$.message", startsWith("Garage is full")));
    }
}
