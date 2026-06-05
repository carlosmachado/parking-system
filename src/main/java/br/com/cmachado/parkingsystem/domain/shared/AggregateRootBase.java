package br.com.cmachado.parkingsystem.domain.shared;

import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots that collect domain events.
 *
 * <p>Events registered through {@link #registerEvent(DomainEvent)} are exposed to Spring
 * Data via {@link DomainEvents} and published automatically when the aggregate is saved,
 * then cleared by {@link AfterDomainEventPublication}.</p>
 *
 * @param <T> the concrete aggregate type
 */
@MappedSuperclass
public abstract class AggregateRootBase<T> implements AggregateRoot<T> {

    @Transient
    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Records a domain event to be published when this aggregate is persisted.
     */
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    @DomainEvents
    @Override
    public Collection<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @AfterDomainEventPublication
    @Override
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
