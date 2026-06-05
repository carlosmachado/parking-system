package br.com.cmachado.parkingsystem.domain.shared;

public interface Entity<T> {
    boolean sameIdentityAs(T other);
}
