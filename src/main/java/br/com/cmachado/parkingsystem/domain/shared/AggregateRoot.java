package br.com.cmachado.parkingsystem.domain.shared;

import java.util.Collection;

/**
 * Consistency boundary that owns a set of domain events pending publication.
 *
 * @param <T> the concrete aggregate type
 */
public interface AggregateRoot<T> extends Entity<T> {
    Collection<DomainEvent> domainEvents();
    void clearDomainEvents();
}
