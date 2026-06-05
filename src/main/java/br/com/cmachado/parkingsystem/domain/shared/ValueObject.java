package br.com.cmachado.parkingsystem.domain.shared;

import java.io.Serializable;

/**
 * Marker for immutable value objects, compared by attribute value rather than identity.
 *
 * @param <T> the concrete value object type
 */
public interface ValueObject<T> extends Serializable {

    /**
     * @return {@code true} when this value object holds the same attribute values as {@code other}
     */
    boolean sameValueAs(T other);
}
