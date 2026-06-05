package br.com.cmachado.parkingsystem.domain.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an application service that orchestrates a use case: it coordinates aggregates,
 * domain services and repositories within a transaction, but holds no business rules itself.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApplicationService {
}
