package br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueResponse {
    private BigDecimal amount;
    private String currency;
    private String timestamp;
}
