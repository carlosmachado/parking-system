package br.com.cmachado.parkingsystem.domain.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a stateless domain service that encapsulates business logic spanning multiple
 * aggregates or value objects and does not naturally belong to any single one.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DomainService {
}
