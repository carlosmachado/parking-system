package br.com.cmachado.parkingsystem.domain.model.revenue.events;

import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenue;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class DailyRevenueCreated extends DomainEvent {

    private final DailyRevenue dailyRevenue;

    public DailyRevenueCreated(DailyRevenue dailyRevenue) {
        super(dailyRevenue);
        this.dailyRevenue = dailyRevenue;
    }
}
