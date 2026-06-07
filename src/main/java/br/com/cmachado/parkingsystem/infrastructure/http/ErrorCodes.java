package br.com.cmachado.parkingsystem.infrastructure.http;

public final class ErrorCodes {

    public static final String REQUEST_VALIDATION = "WEB-001";
    public static final String GARAGE_FULL = "EST-001";
    public static final String INVALID_PARKING_SESSION_STATE = "EST-002";
    public static final String PARKING_SPOT_OCCUPIED = "EST-003";
    public static final String PARKING_SESSION_NOT_FOUND = "EST-004";
    public static final String PARKING_SPOT_NOT_FOUND = "EST-005";
    public static final String PARKING_SESSION_ALREADY_EXITED = "EST-006";
    public static final String UNEXPECTED = "SYS-001";

    private ErrorCodes() {
    }
}
