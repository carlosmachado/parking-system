package br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue;

import br.com.cmachado.parkingsystem.application.revenue.RevenueApplicationService;
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
    private RevenueApplicationService revenueService;

    @Test
    void returnsRevenueForGivenSectorAndDate() throws Exception {
        RevenueResponse response = RevenueResponse.builder()
                .amount(new BigDecimal("42.00"))
                .currency("BRL")
                .timestamp("2025-01-01T12:00:00")
                .build();
        when(revenueService.getRevenue(LocalDate.parse("2025-01-01"), "A")).thenReturn(response);

        mockMvc.perform(get("/revenue").param("date", "2025-01-01").param("sector", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(42.00))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void aggregatesAllSectorsWhenSectorOmitted() throws Exception {
        RevenueResponse response = RevenueResponse.builder()
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .timestamp("2025-01-01T12:00:00")
                .build();
        when(revenueService.getRevenueAllSectors(LocalDate.parse("2025-01-01"))).thenReturn(response);

        mockMvc.perform(get("/revenue").param("date", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.00));

        verify(revenueService).getRevenueAllSectors(eq(LocalDate.parse("2025-01-01")));
    }

    @Test
    void defaultsToTodayWhenDateOmitted() throws Exception {
        RevenueResponse response = RevenueResponse.builder()
                .amount(BigDecimal.ZERO)
                .currency("BRL")
                .timestamp("2025-01-01T12:00:00")
                .build();
        when(revenueService.getRevenueAllSectors(LocalDate.now())).thenReturn(response);

        mockMvc.perform(get("/revenue"))
                .andExpect(status().isOk());

        verify(revenueService).getRevenueAllSectors(eq(LocalDate.now()));
    }
}
