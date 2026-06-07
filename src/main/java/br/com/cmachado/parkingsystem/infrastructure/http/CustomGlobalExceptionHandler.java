package br.com.cmachado.parkingsystem.infrastructure.http;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.CantParkSessionException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.ParkingSessionAlreadyExitedException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.ParkingSpotOccupiedException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.GarageFullException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class CustomGlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_VALIDATION, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ErrorCodes.INVALID_PARKING_SESSION_STATE, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(CantParkSessionException.class)
    public ResponseEntity<ApiError> handleCantParkSessionException(CantParkSessionException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ParkingSpotOccupiedException.class)
    public ResponseEntity<ApiError> handleParkingSpotOccupiedException(ParkingSpotOccupiedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ParkingSessionAlreadyExitedException.class)
    public ResponseEntity<ApiError> handleParkingSessionAlreadyExitedException(ParkingSessionAlreadyExitedException ex,
                                                                              HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
                                                                         HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_VALIDATION, message, request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_VALIDATION, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex,
                                                                                 HttpServletRequest request) {
        String message = "Request parameter '%s' is required".formatted(ex.getParameterName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_VALIDATION, message, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex,
                                                                             HttpServletRequest request) {
        String message = "Request parameter '%s' has invalid value '%s'".formatted(ex.getName(), ex.getValue());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_VALIDATION, message, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
                                                                         HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_VALIDATION, "Request body is invalid", request.getRequestURI());
    }

    @ExceptionHandler(GarageFullException.class)
    public ResponseEntity<ApiError> handleGarageFullException(GarageFullException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.UNEXPECTED, "An unexpected error occurred", request.getRequestURI());
    }

    private String formatFieldError(FieldError fieldError) {
        return "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiError> buildErrorResponse(HttpStatus status, String code, String message, String path) {
        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(path)
                .build();
        return new ResponseEntity<>(apiError, status);
    }
}
