package br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue;

import br.com.cmachado.parkingsystem.application.revenue.RevenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RevenueRestController.class)
class RevenueRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevenueService revenueService;

    @Test
    void returnsRevenueForGivenSectorAndDate() throws Exception {
        // arrange
        when(revenueService.getRevenue(LocalDate.parse("2025-01-01"), "A"))
                .thenReturn(revenueResponse("42.00"));

        // act / assert
        mockMvc.perform(get("/revenue").param("date", "2025-01-01").param("sector", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(42.00))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void aggregatesAllSectorsWhenSectorOmitted() throws Exception {
        // arrange
        when(revenueService.getRevenueAllSectors(LocalDate.parse("2025-01-01")))
                .thenReturn(revenueResponse("100.00"));

        // act
        mockMvc.perform(get("/revenue").param("date", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.00));

        // assert
        verify(revenueService).getRevenueAllSectors(eq(LocalDate.parse("2025-01-01")));
    }

    @Test
    void defaultsToTodayWhenDateOmitted() throws Exception {
        // arrange
        when(revenueService.getRevenueAllSectors(LocalDate.now())).thenReturn(revenueResponse("0.00"));

        // act
        mockMvc.perform(get("/revenue"))
                .andExpect(status().isOk());

        // assert
        verify(revenueService).getRevenueAllSectors(eq(LocalDate.now()));
    }

    private RevenueResponse revenueResponse(String amount) {
        return RevenueResponse.builder()
                .amount(new BigDecimal(amount))
                .currency("BRL")
                .timestamp("2025-01-01T12:00:00")
                .build();
    }
}
