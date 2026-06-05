package br.com.cmachado.parkingsystem.domain.shared;

import org.springframework.context.ApplicationEvent;

/**
 * Base type for domain events. Extends Spring's {@link ApplicationEvent} so events can be
 * published by aggregates and consumed by transactional/async listeners.
 */
public abstract class DomainEvent extends ApplicationEvent {
    public DomainEvent(Object source) {
        super(source);
    }
}
